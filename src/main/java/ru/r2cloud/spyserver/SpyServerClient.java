package ru.r2cloud.spyserver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class SpyServerClient {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerClient.class);
	private static final int SPYSERVER_CMD_HELLO = 0;
	private static final int SPYSERVER_CMD_SET_SETTING = 2;

	private static final int SPYSERVER_MSG_TYPE_DEVICE_INFO = 0;
	private static final int SPYSERVER_MSG_TYPE_CLIENT_SYNC = 1;

	private static final int SPYSERVER_PROTOCOL_VERSION = (((2) << 24) | ((0) << 16) | (1700));
	private static final String CLIENT_ID = "r2cloud";

	private final String host;
	private final int port;
	private final int socketTimeout;

	private Socket socket;
	private SpyServerStatus status;
	private CommandResponse response;
	private SpyServerClientSync sync;
	private Object lock = new Object();

	public SpyServerClient(String host, int port, int socketTimeout) {
		this.host = host;
		this.port = port;
		this.socketTimeout = socketTimeout;
	}

	public void start() throws IOException {
		socket = new Socket(host, port);
		socket.setSoTimeout(socketTimeout);
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!socket.isClosed()) {
					// FIXME reconnect
					synchronized (lock) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							break;
						}
						if (socket.isClosed()) {
							break;
						}
						try {
							InputStream inputStream = socket.getInputStream();
							ResponseHeader header = ResponseHeader.read(inputStream);
							if (header.getMessageType() == SPYSERVER_MSG_TYPE_DEVICE_INFO) {
								response = new SpyServerDeviceInfo();
								response.read(inputStream);
								LOG.info("spyserver connected: {}", response.toString());
								// initial sync contains 2 responses
								header = ResponseHeader.read(inputStream);
							}
							if (header.getMessageType() == SPYSERVER_MSG_TYPE_CLIENT_SYNC) {
								if (response != null) {
									sync = new SpyServerClientSync();
									sync.read(inputStream);
								} else {
									response = new SpyServerClientSync();
									response.read(inputStream);
								}
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
	}

	public SpyServerStatus getStatus() {
		// cache status because handshake between client and spyserver can happen only
		// once
		if (status != null) {
			return status;
		}
		SpyServerStatus result = new SpyServerStatus();
		try {
			SpyServerDeviceInfo response = (SpyServerDeviceInfo) sendCommand(SPYSERVER_CMD_HELLO, new CommandHello(SPYSERVER_PROTOCOL_VERSION, CLIENT_ID));
			if (response != null) {
				result.setStatus(DeviceConnectionStatus.CONNECTED);
				result.setMinFrequency(response.getMinimumFrequency());
				result.setMaxFrequency(response.getMaximumFrequency());
			}
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to get status", e);
			result.setFailureMessage(e.getMessage());
			result.setStatus(DeviceConnectionStatus.FAILED);
		}
		status = result;
		return result;
	}

	public void setParameter(SpyServerParameter parameter, long value) throws IOException {
		try {
			sendCommand(SPYSERVER_CMD_SET_SETTING, new CommandSetParameter(parameter.getCode(), value));
		} catch (IOException e) {
			if (status != null) {
				status.setStatus(DeviceConnectionStatus.FAILED);
				status.setFailureMessage(e.getMessage());
			}
			throw e;
		}
	}

	private CommandResponse sendCommand(int commandType, CommandRequest command) throws IOException {
		byte[] body = command.toByteArray();
		synchronized (lock) {
			response = null;
			LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(new DataOutputStream(socket.getOutputStream()));
			out.writeInt(commandType);
			out.writeInt(body.length);
			out.write(body);
			out.flush();
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

	public void stop() {
		LOG.info("stopping spyserver client...");
		if (socket != null) {
			Util.closeQuietly(socket);
		}
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public static void main(String[] args) throws Exception {
		String host = "192.168.18.15";
		int port = 5555;
		SpyServerClient client = new SpyServerClient(host, port, 10000);
		client.start();
		SpyServerStatus status = client.getStatus();
		System.out.println(status.getStatus());
		System.out.println(status.getFailureMessage());
		System.out.println(status.getMinFrequency());
		System.out.println(status.getMaxFrequency());
		client.setParameter(SpyServerParameter.SPYSERVER_SETTING_IQ_FREQUENCY, 434000000);
		Thread.sleep(10000);
		client.stop();

	}

}
