package ru.r2cloud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SatnogsServerMock {

	private static final Pattern EQ = Pattern.compile("=");

	private HttpServer server;
	private Set<String> pathsConfigured = new HashSet<>();
	private String tleBase;
	
	public SatnogsServerMock() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 8007), 0);
	}

	public void setTleMockDirectory(String base) {
		this.tleBase = base;
	}

	public void setSatellitesMock(String message, int code) {
		HttpHandler mock = new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(code, message.length());
				OutputStream os = exchange.getResponseBody();
				os.write(message.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		};
		addHandler(mock, "/api/satellites/");
	}

	public void setSatellitesMock(HttpHandler mock) {
		addHandler(mock, "/api/satellites/");
	}

	public void setTransmittersMock(String message, int code) {
		HttpHandler mock = new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(code, message.length());
				OutputStream os = exchange.getResponseBody();
				os.write(message.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		};
		addHandler(mock, "/api/transmitters/");
	}

	public void setTransmittersMock(HttpHandler mock) {
		addHandler(mock, "/api/transmitters/");
	}

	private void addHandler(HttpHandler mock, String path) {
		if (!pathsConfigured.add(path)) {
			server.removeContext(path);
		}
		server.createContext(path, mock);
	}

	public void start() {
		HttpHandler plainHandler = new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				URI req = exchange.getRequestURI();
				String[] parts = EQ.split(req.getQuery());
				if (parts.length == 2) {
					String filepath = tleBase + "/" + parts[1] + ".json";
					boolean filepathExists;
					try (InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(filepath)) {
						filepathExists = (is != null);
					} catch (Exception e) {
						filepathExists = false;
					}
					String responseBody;
					if (filepathExists) {
						responseBody = TestUtil.loadExpected(filepath);
					} else {
						responseBody = "[]";
					}
					exchange.getResponseHeaders().add("Content-Type", "application/json");
					exchange.sendResponseHeaders(200, responseBody.length());
					OutputStream os = exchange.getResponseBody();
					os.write(responseBody.getBytes(StandardCharsets.UTF_8));
					os.close();
				} else {
					exchange.sendResponseHeaders(403, 0);
				}
			}
		};
		addHandler(plainHandler, "/api/tle/");
		server.start();
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	public String getUrl() {
		return "http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort();
	}
}
