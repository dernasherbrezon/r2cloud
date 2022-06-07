package ru.r2cloud.sdrmodem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdrModemMock {

	private static final Logger LOG = LoggerFactory.getLogger(SdrModemMock.class);

	private final int port;

	private ServerSocket socket;
	private Thread thread;
	private SdrModemHandler handler;

	public SdrModemMock(int port) {
		this.port = port;
	}

	public void setHandler(SdrModemHandler handler) {
		this.handler = handler;
	}

	public void start() throws IOException {
		socket = new ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"));
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Socket client = socket.accept();
						handler.handleClient(client);
					} catch (IOException e) {
						return;
					}
				}
				LOG.info("shutting down");
			}

		}, "client-handler");
		thread.start();
	}

	public void stop() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				LOG.error("unable to close socket", e);
			}
		}
		if (thread != null) {
			thread.interrupt();
		}
	}

}
