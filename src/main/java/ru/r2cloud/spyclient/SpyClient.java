package ru.r2cloud.spyclient;

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

public class SpyClient {

	private static final Logger LOG = LoggerFactory.getLogger(SpyClient.class);
	public static final int SPYSERVER_CMD_HELLO = 0;
	public static final int SPYSERVER_CMD_SET_SETTING = 2;

	private static final int SPYSERVER_STREAM_MODE_IQ_ONLY = 1;

	private static final int SPYSERVER_STREAM_FORMAT_INVALID = 0;
	private static final int SPYSERVER_STREAM_FORMAT_UINT8 = 1;
	private static final int SPYSERVER_STREAM_FORMAT_INT16 = 2;
	private static final int SPYSERVER_STREAM_FORMAT_FLOAT = 4;

	public static final int SPYSERVER_MSG_TYPE_DEVICE_INFO = 0;
	public static final int SPYSERVER_MSG_TYPE_CLIENT_SYNC = 1;
	public static final int SPYSERVER_MSG_TYPE_UINT8_IQ = 100;
	public static final int SPYSERVER_MSG_TYPE_INT16_IQ = 101;
	public static final int SPYSERVER_MSG_TYPE_FLOAT_IQ = 103;

	private static final int SPYSERVER_PROTOCOL_VERSION = (((2) << 24) | ((0) << 16) | (1700));
	private static final String CLIENT_ID = "r2cloud";

	private final String host;
	private final int port;
	private final int socketTimeout;
	private final Map<Long, Long> supportedSamplingRates = new HashMap<>();
	private final Object lock = new Object();

	private Socket socket;
	private OutputStream socketOut;
	private SpyServerStatus status;

	private SpyServerDeviceInfo deviceInfo;
	private SpyClientSync sync;
	private OnDataCallback callback;

	public SpyClient(String host, int port, int socketTimeout) {
		this.host = host;
		this.port = port;
		this.socketTimeout = socketTimeout;
	}

	public void start() throws IOException {
		socket = new Socket(host, port);
		socket.setSoTimeout(socketTimeout);
		socketOut = socket.getOutputStream();
		new Thread(new Runnable() {

			@Override
			public void run() {
				InputStream inputStream;
				try {
					inputStream = socket.getInputStream();
				} catch (IOException e) {
					Util.logIOException(LOG, "unable to read response", e);
					markStatusAsFailed(e);
					return;
				}
				ResponseHeader responseHeader = new ResponseHeader();
				while (!socket.isClosed()) {
					try {
						responseHeader.read(inputStream);
						if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_DEVICE_INFO) {
							synchronized (lock) {
								deviceInfo = new SpyServerDeviceInfo();
								deviceInfo.read(inputStream);
								LOG.info("spyserver connected: {}", deviceInfo.toString());
								lock.notifyAll();
							}
						} else if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_CLIENT_SYNC) {
							synchronized (lock) {
								sync = new SpyClientSync();
								sync.read(inputStream);
								LOG.info("state: {}", sync.toString());
								lock.notifyAll();
							}
						} else if (responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_INT16_IQ || responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_FLOAT_IQ || responseHeader.getMessageType() == SPYSERVER_MSG_TYPE_UINT8_IQ) {
							synchronized (lock) {
								if (callback != null) {
									if (!callback.onData(inputStream, (int) responseHeader.getBodySize())) {
										stopStream();
									}
								} else {
									inputStream.skipNBytes(responseHeader.getBodySize());
								}
							}
						} else {
							LOG.info("unknown message received: {}", responseHeader.getMessageType());
							inputStream.skipNBytes(responseHeader.getBodySize());
						}
					} catch (EOFException e) {
						LOG.info("spyserver disconnected");
						stop();
						break;
					} catch (IOException e) {
						if (socket != null) {
							try {
								if (!socket.isClosed() && inputStream.available() == 0) {
									continue;
								}
								if (!socket.isClosed()) {
									Util.logIOException(LOG, "unable to read response", e);
									markStatusAsFailed(e);
								}
							} catch (IOException e1) {
								Util.logIOException(LOG, "unable to read response", e);
								markStatusAsFailed(e);
							}
						}
						stop();
						break;
					}
				}
				LOG.info("spyclient stopped");
			}

		}, "spyclient").start();
		sendCommandAsync(SPYSERVER_CMD_HELLO, new CommandHello(SPYSERVER_PROTOCOL_VERSION, CLIENT_ID));
		synchronized (lock) {
			int remainingMillis = socketTimeout;
			while (deviceInfo == null || sync == null) {
				if (remainingMillis <= 0) {
					status = new SpyServerStatus();
					status.setStatus(DeviceConnectionStatus.FAILED);
					status.setFailureMessage("Device is not connected");
					throw new IOException("timeout waiting for sync");
				}
				long start = System.currentTimeMillis();
				try {
					lock.wait(remainingMillis);
				} catch (InterruptedException e) {
					status = new SpyServerStatus();
					status.setStatus(DeviceConnectionStatus.FAILED);
					status.setFailureMessage("Device is not connected");
					Thread.currentThread().interrupt();
					return;
				}
				remainingMillis -= (System.currentTimeMillis() - start);
			}
			status = new SpyServerStatus();
			// cache status because handshake between client and spyserver can happen only
			// once
			status.setStatus(DeviceConnectionStatus.CONNECTED);
			status.setMinFrequency(deviceInfo.getMinimumFrequency());
			status.setMaxFrequency(deviceInfo.getMaximumFrequency());
			status.setFormat(resolveDataFormat(deviceInfo));
			sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_MODE.getCode(), SPYSERVER_STREAM_MODE_IQ_ONLY));
			sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FORMAT.getCode(), convertDataFormat(status.getFormat())));
			// rtl-sdr won't work without this setting
			sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_DIGITAL_GAIN.getCode(), 0xffffffffL));
			for (long i = deviceInfo.getMinimumIQDecimation(); i < deviceInfo.getDecimationStageCount(); i++) {
				supportedSamplingRates.put(deviceInfo.getMaximumSampleRate() / (1 << i), i);
			}

			List<Long> supportedList = new ArrayList<>(supportedSamplingRates.keySet());
			Collections.sort(supportedList);
			status.setSupportedSampleRates(supportedList);
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
		sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_GAIN.getCode(), value));
	}

	public void setFrequency(long value) throws IOException {
		if (status == null || status.getStatus() != DeviceConnectionStatus.CONNECTED) {
			throw new IOException("not connected");
		}
		sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FREQUENCY.getCode(), value));
	}

	public void setSamplingRate(long value) throws IOException {
		if (status == null || status.getStatus() != DeviceConnectionStatus.CONNECTED) {
			throw new IOException("not connected");
		}
		Long decimation = supportedSamplingRates.get(value);
		if (decimation == null) {
			throw new IOException("sampling rate is not supported: " + value);
		}
		sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_DECIMATION.getCode(), decimation));
	}

	public List<Long> getSupportedSamplingRates() {
		List<Long> result = new ArrayList<>(supportedSamplingRates.keySet());
		Collections.sort(result);
		return result;
	}

	public void startStream(OnDataCallback callback) throws IOException {
		synchronized (lock) {
			this.callback = callback;
		}
		sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_ENABLED.getCode(), 1));
	}

	public void stopStream() throws IOException {
		synchronized (lock) {
			this.callback = null;
		}
		sendCommandAsync(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(SpyServerParameter.SPYSERVER_SETTING_STREAMING_ENABLED.getCode(), 0));
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

	private void markStatusAsFailed(IOException e) {
		synchronized (lock) {
			if (status == null) {
				status = new SpyServerStatus();
			}
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage(e.getMessage());
		}
	}

	public void stop() {
		if (socket == null) {
			return;
		}
		LOG.info("stopping spyclient...");
		if (socket != null) {
			Util.closeQuietly(socket);
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
