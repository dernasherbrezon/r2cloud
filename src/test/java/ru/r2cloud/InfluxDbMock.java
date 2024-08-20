package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class InfluxDbMock {

	private static final Pattern EQ = Pattern.compile("=");

	private HttpServer server;
	private Set<String> pathsConfigured = new HashSet<>();
	private final Map<String, List<String>> metricsByDatabase = new ConcurrentHashMap<>();
	private final CountDownLatch latch = new CountDownLatch(1);

	public InfluxDbMock() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 8009), 0);
	}

	private void addHandler(HttpHandler mock, String path) {
		if (!pathsConfigured.add(path)) {
			server.removeContext(path);
		}
		server.createContext(path, mock);
	}

	public void start() {
		addHandler(new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				URI req = exchange.getRequestURI();
				String[] parts = EQ.split(req.getQuery());
				if (parts.length == 2) {
					String database = parts[1];
					List<String> previous = metricsByDatabase.get(database);
					if (previous == null) {
						previous = new ArrayList<>();
						metricsByDatabase.put(database, previous);
					}
					BufferedReader r = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
					String curLine = null;
					while ((curLine = r.readLine()) != null) {
						previous.add(curLine);
					}
					String message = "{}";
					exchange.getResponseHeaders().add("Content-Type", "application/json");
					exchange.sendResponseHeaders(200, message.length());
					OutputStream os = exchange.getResponseBody();
					os.write(message.getBytes(StandardCharsets.UTF_8));
					os.close();
				} else {
					exchange.sendResponseHeaders(403, 0);
				}
				latch.countDown();
			}
		}, "/write");
		server.start();
	}

	public void waitForMetric() throws InterruptedException {
		latch.await();
	}

	public Map<String, List<String>> getMetricsByDatabase() {
		return metricsByDatabase;
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	public String getHostname() {
		return "http://" + server.getAddress().getHostName();
	}

	public int getPort() {
		return server.getAddress().getPort();
	}

}
