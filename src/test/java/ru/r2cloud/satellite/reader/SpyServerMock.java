package ru.r2cloud.satellite.reader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.spyclient.ResponseHeader;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyClientSync;
import ru.r2cloud.spyclient.SpyServerDeviceInfo;
import ru.r2cloud.spyclient.SpyServerParameter;
import ru.r2cloud.util.Util;

public class SpyServerMock {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerMock.class);
	private final String host;
	private int port = 8008;
	private ServerSocket socket;
	private SpyServerDeviceInfo deviceInfo;
	private SpyClientSync sync;
	private byte[] data;
	private int dataType;
	private CountDownLatch latch = new CountDownLatch(1);
	private Map<SpyServerParameter, Long> params = new HashMap<>();

	public SpyServerMock(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setDeviceInfo(SpyServerDeviceInfo deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public void setSync(SpyClientSync sync) {
		this.sync = sync;
	}

	public Long getParameter(SpyServerParameter param) {
		return params.get(param);
	}

	public void setData(byte[] data, int dataType) {
		this.data = data;
		this.dataType = dataType;
	}

	public void waitForDataSent() throws InterruptedException {
		latch.await();
	}

	public void start() throws IOException {
		socket = new ServerSocket();
		socket.setReuseAddress(true);
		for (int i = 0; i < 10; i++) {
			try {
				socket.bind(new InetSocketAddress(host, port));
				break;
			} catch (IOException e) {
				LOG.info("can't bind on: {} trying next. Error: {}", port, e.getMessage());
				port++;
			}
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
					try {
						Socket client = socket.accept();
						InputStream is = client.getInputStream();
						OutputStream os = client.getOutputStream();
						while (!client.isClosed() && !Thread.currentThread().isInterrupted()) {
							try {
								long messageType = SpyClient.readUnsignedInt(is);
								long messageLength = SpyClient.readUnsignedInt(is);
								if (messageType == SpyClient.SPYSERVER_CMD_HELLO) {
									long protocolVersion = SpyClient.readUnsignedInt(is);
									byte[] clientIdBytes = new byte[(int) messageLength - 4];
									is.read(clientIdBytes);
									LOG.info("client connected: {} version: {}", new String(clientIdBytes, StandardCharsets.US_ASCII), protocolVersion);
									if (deviceInfo != null) {
										// only message type is important
										ResponseHeader header = new ResponseHeader();
										header.setMessageType(SpyClient.SPYSERVER_MSG_TYPE_DEVICE_INFO);
										header.write(os);
										deviceInfo.write(os);
									} else {
										break;
									}
									if (sync != null) {
										// only message type is important
										ResponseHeader header = new ResponseHeader();
										header.setMessageType(SpyClient.SPYSERVER_MSG_TYPE_CLIENT_SYNC);
										header.write(os);
										sync.write(os);
									}
								}
								if (messageType == SpyClient.SPYSERVER_CMD_SET_SETTING) {
									SpyServerParameter param = SpyServerParameter.valueOfCode((int) SpyClient.readUnsignedInt(is));
									if (param == null) {
										continue;
									}
									long value = SpyClient.readUnsignedInt(is);
									params.put(param, value);
									if (param == SpyServerParameter.SPYSERVER_SETTING_STREAMING_ENABLED && value == 1 && data != null) {
										ResponseHeader header = new ResponseHeader();
										header.setMessageType(dataType);
										header.setBodySize(data.length);
										header.write(os);
										os.write(data);
										latch.countDown();
									}
								}
							} catch (EOFException e) {
								break;
							} catch (IOException e) {
								if (!socket.isClosed() && is.available() == 0) {
									continue;
								}
								break;
							}
						}
						Util.closeQuietly(client);
					} catch (IOException e) {
						LOG.info("shutdown: {}", e.getMessage());
						break;
					}
				}
				LOG.info("stopped");
			}
		}, "spyserver-mock").start();
	}

	public void stop() {
		if (socket != null) {
			Util.closeQuietly(socket);
		}
	}

}
