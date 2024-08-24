package ru.r2cloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactoryImpl;
import ru.r2cloud.util.Util;

public class GpsdMock {

	private final static Logger LOG = LoggerFactory.getLogger(GpsdMock.class);

	private ServerSocket socket;
	private Thread thread;
	private ScheduledExecutorService executor = null;
	private List<JsonObject> response;

	public void setResponse(List<JsonObject> response) {
		this.response = response;
	}

	public void start() throws IOException {
		executor = new ThreadPoolFactoryImpl(1000).newScheduledThreadPool(2, new NamingThreadFactory("gpsd-mock"));
		socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress("127.0.0.1", 2947));
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Socket client = socket.accept();
						executor.execute(new Runnable() {
							@Override
							public void run() {
								handleClient(client);
							}
						});
					} catch (IOException e) {
						return;
					}
				}
				LOG.info("shutting down");
			}

		}, "gpsd-mock");
		thread.start();
	}

	private void handleClient(Socket client) {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String request = null;
				while (!Thread.currentThread().isInterrupted() && (request = reader.readLine()) != null) {
					if (request.startsWith("?WATCH")) {
						break;
					}
				}
				if (request == null) {
					return;
				}

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
				for (JsonObject cur : response) {
					writer.append(cur.toString()).append('\n');
					writer.flush();
				}
			} catch (IOException e) {
				Util.logIOException(LOG, false, "client has been disconnected", e);
				break;
			}
		}
	}

	public void stop() {
		Util.closeQuietly(socket);
		if (thread != null) {
			thread.interrupt();
		}
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	public String getHostname() {
		return socket.getInetAddress().getHostName();
	}

	public int getPort() {
		return socket.getLocalPort();
	}

}
