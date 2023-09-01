package ru.r2cloud.spyserver;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class SpyServerClient {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerClient.class);
	private static final int SPYSERVER_CMD_HELLO = 0;
	private static final int SPYSERVER_CMD_SET_SETTING = 2;

	private static final int SPYSERVER_STREAM_MODE_IQ_ONLY = 1;

	private static final int SPYSERVER_STREAM_FORMAT_INVALID = 0;
	private static final int SPYSERVER_STREAM_FORMAT_UINT8 = 1;
	private static final int SPYSERVER_STREAM_FORMAT_INT16 = 2;
	private static final int SPYSERVER_STREAM_FORMAT_FLOAT = 4;

	private static final int SPYSERVER_MSG_TYPE_DEVICE_INFO = 0;
	private static final int SPYSERVER_MSG_TYPE_CLIENT_SYNC = 1;
	private static final int SPYSERVER_MSG_TYPE_UINT8_IQ = 100;
	private static final int SPYSERVER_MSG_TYPE_INT16_IQ = 101;
	private static final int SPYSERVER_MSG_TYPE_FLOAT_IQ = 103;

	private static final int SPYSERVER_PROTOCOL_VERSION = (((2) << 24) | ((0) << 16) | (1700));
	private static final String CLIENT_ID = "r2cloud";

	private final String host;
	private final int port;
	private final int socketTimeout;

	private Socket socket;
	private OutputStream socketOut;
	private SpyServerStatus status;

	private ResponseHeader responseHeader = new ResponseHeader();
	private CommandResponse response;
	private SpyServerClientSync sync;
	private byte[] buffer = new byte[4096];
	private final Map<Long, Long> supportedSamplingRates = new HashMap<>();
	private OnDataCallback callback;
	private Object lock = new Object();
	long totalSamples = 0;

	public SpyServerClient(String host, int port, int socketTimeout) {
		this.host = host;
		this.port = port;
		this.socketTimeout = socketTimeout;
	}

	public void start() throws IOException {
		socket = new Socket(host, port);
		socket.setSoTimeout(socketTimeout);
		socket.setTcpNoDelay(true);
		socketOut = socket.getOutputStream();
		new Thread(new Runnable() {

			@Override
			public void run() {
				InputStream inputStream;
				try {
					inputStream = socket.getInputStream();
				} catch (IOException e) {
					Util.logIOException(LOG, "unable to read response", e);
					return;
				}
				while (!socket.isClosed()) {
					// FIXME reconnect
					synchronized (lock) {
						if (callback == null) {
							try {
								lock.wait();
							} catch (InterruptedException e) {
								break;
							}
						}
						if (socket.isClosed()) {
							break;
						}
						try {
							responseHeader.read(inputStream);
							if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_DEVICE_INFO) {
								response = new SpyServerDeviceInfo();
								response.read(inputStream);
								LOG.info("spyserver connected: {}", response.toString());
								// initial sync contains 2 responses
								responseHeader.read(inputStream);
							}
							if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_CLIENT_SYNC) {
								if (response != null) {
									sync = new SpyServerClientSync();
									sync.read(inputStream);
									LOG.info("state: {}", sync.toString());
								} else {
									response = new SpyServerClientSync();
									response.read(inputStream);
									LOG.info("state: {}", response.toString());
								}
							} else if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_INT16_IQ || responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_FLOAT_IQ || responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_UINT8_IQ) {
								if (callback != null) {
									callback.onData(inputStream, (int) responseHeader.getBodySize());
								} else {
									inputStream.skipNBytes(responseHeader.getBodySize());
								}
							} else {
								LOG.info("unknown message received: {}", responseHeader.getMessageType());
								inputStream.skipNBytes(responseHeader.getBodySize());
							}
						} catch (IOException e) {
							Util.logIOException(LOG, "unable to read response", e);
							break;
						} finally {
							lock.notifyAll();
						}
					}
				}
				LOG.info("spyserver client stopped");
			}
		}, "spyserver-client").start();
		status = new SpyServerStatus();
		SpyServerDeviceInfo response = (SpyServerDeviceInfo) sendCommandSync(SPYSERVER_CMD_HELLO, new CommandHello(SPYSERVER_PROTOCOL_VERSION, CLIENT_ID));
		// cache status because handshake between client and spyserver can happen only
		// once
		if (response != null) {
			status.setStatus(DeviceConnectionStatus.CONNECTED);
			status.setMinFrequency(response.getMinimumFrequency());
			status.setMaxFrequency(response.getMaximumFrequency());
			status.setFormat(resolveDataFormat(response));
			setParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_MODE, SPYSERVER_STREAM_MODE_IQ_ONLY);
			setParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FORMAT, convertDataFormat(status.getFormat()));
			for (long i = response.getMinimumIQDecimation(); i < response.getDecimationStageCount(); i++) {
				supportedSamplingRates.put(response.getMaximumSampleRate() / (1 << i), i);
			}
		} else {
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage("Device is not connected");
		}
	}

	public SpyServerStatus getStatus() {
		return status;
	}

	public void setGain(long value) throws IOException {
		if (status == null || status.getStatus() != DeviceConnectionStatus.CONNECTED) {
			throw new IOException("not connected");
		}
		if (sync != null && sync.getCanControl() == 0) {
			LOG.info("can't control gain. will use server-default: {}", sync.getGain());
			return;
		}
		setParameter(SpyServerParameter.SPYSERVER_SETTING_GAIN, value);
	}

	public void setFrequency(long value) throws IOException {
		if (status == null || status.getStatus() != DeviceConnectionStatus.CONNECTED) {
			throw new IOException("not connected");
		}
		setParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FREQUENCY, value);
	}

	public void setSamplingRate(long value) throws IOException {
		if (status == null || status.getStatus() != DeviceConnectionStatus.CONNECTED) {
			throw new IOException("not connected");
		}
		Long decimation = supportedSamplingRates.get(value);
		if (decimation == null) {
			throw new IOException("sampling rate is not supported: " + value);
		}
		setParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_DECIMATION, decimation);
	}

	public List<Long> getSupportedSamplingRates() {
		List<Long> result = new ArrayList<>(supportedSamplingRates.keySet());
		Collections.sort(result);
		return result;
	}

	private void setParameter(SpyServerParameter parameter, long value) throws IOException {
		try {
			if (parameter.isAsync()) {
				sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(parameter.getCode(), value));
			} else {
				sendCommandSync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(parameter.getCode(), value));
			}
		} catch (IOException e) {
			if (status != null) {
				status.setStatus(DeviceConnectionStatus.FAILED);
				status.setFailureMessage(e.getMessage());
			}
			throw e;
		}
	}

	public void startStream(OnDataCallback callback) throws IOException {
		synchronized (lock) {
			this.callback = callback;
			setParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FORMAT, convertDataFormat(status.getFormat()));
			setParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_ENABLED, 1);
			lock.notifyAll();
		}
	}

	public void stopStream() throws IOException {
		synchronized (lock) {
			setParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_ENABLED, 0);
			this.callback = null;
		}
	}

	private CommandResponse sendCommandSync(int commandType, CommandRequest command) throws IOException {
		byte[] body = command.toByteArray();
		synchronized (lock) {
			response = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writeUnsignedInt(baos, commandType);
			writeUnsignedInt(baos, body.length);
			writeUnsignedInt(baos, body.length);
			baos.write(body);
			baos.close();
			socketOut.write(baos.toByteArray());
			socketOut.flush();
			lock.notifyAll();
			try {
				lock.wait(socketTimeout);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			return response;
		}
	}

	private void sendCommandAsync(int commandType, CommandRequest command) throws IOException {
		byte[] body = command.toByteArray();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeUnsignedInt(baos, commandType);
		writeUnsignedInt(baos, body.length);
		baos.write(body);
		baos.close();
		socketOut.write(baos.toByteArray());
		socketOut.flush();
	}

	public void stop() {
		LOG.info("stopping spyserver client...");
		if (socket != null) {
			Util.closeQuietly(socket);
		}
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	private static DataFormat resolveDataFormat(SpyServerDeviceInfo response) throws IOException {
		if (response.getForcedIQFormat() != SPYSERVER_STREAM_FORMAT_INVALID) {
			switch ((int) response.getForcedIQFormat()) {
			case SPYSERVER_STREAM_FORMAT_UINT8:
				return DataFormat.COMPLEX_UNSIGNED_BYTE;
			case SPYSERVER_STREAM_FORMAT_INT16:
				return DataFormat.COMPLEX_SIGNED_SHORT;
			case SPYSERVER_STREAM_FORMAT_FLOAT:
				return DataFormat.COMPLEX_FLOAT;
			default:
				throw new IOException("unsupported forced data format: " + response.getForcedIQFormat());
			}
		} else {
			switch ((int) response.getResolution()) {
			case 8:
				// rtl-sdr
				return DataFormat.COMPLEX_UNSIGNED_BYTE;
			case 12:
			case 16:
				// air-spy
				return DataFormat.COMPLEX_SIGNED_SHORT;
			case 32:
				// something else
				return DataFormat.COMPLEX_FLOAT;
			default:
				throw new IOException("unsupported resolution: " + response.getResolution());
			}
		}
	}

	private static int convertDataFormat(DataFormat format) throws IOException {
		switch (format) {
		case COMPLEX_FLOAT:
			return SPYSERVER_STREAM_FORMAT_FLOAT;
		case COMPLEX_SIGNED_SHORT:
			return SPYSERVER_STREAM_FORMAT_INT16;
		case COMPLEX_UNSIGNED_BYTE:
			return SPYSERVER_STREAM_FORMAT_UINT8;
		default:
			throw new IOException("unsupported format: " + format);
		}
	}

	public static long readUnsignedInt(InputStream is) throws IOException {
		int ch1 = is.read();
		int ch2 = is.read();
		int ch3 = is.read();
		int ch4 = is.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch4 << 24) | (ch3 << 16) | (ch2 << 8) | ch1) & 0xFFFFFFFFL;
	}

	public static void writeUnsignedInt(OutputStream os, int v) throws IOException {
		os.write(0xFF & v);
		os.write(0xFF & (v >> 8));
		os.write(0xFF & (v >> 16));
		os.write(0xFF & (v >> 24));
	}

}
