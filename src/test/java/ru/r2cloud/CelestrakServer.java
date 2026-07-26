package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CelestrakServer {

	private Set<String> mockedPaths = new HashSet<>();
	private HttpServer server;

	public void mockResponse(String queryPath, String data) {
		if (mockedPaths.contains(queryPath)) {
			server.removeContext(queryPath);
		}
		server.createContext(queryPath, new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (data == null) {
					String message = "expected not found";
					byte[] body = message.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(404, body.length);
					OutputStream os = exchange.getResponseBody();
					os.write(body);
					os.close();
					return;
				}
				byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, bytes.length);
				OutputStream os = exchange.getResponseBody();
				os.write(bytes);
				os.close();
			}
		});
		mockedPaths.add(queryPath);
	}

	public void removeMock(String queryPath) {
		if (mockedPaths.remove(queryPath)) {
			server.removeContext(queryPath);
		}
	}

	public void start() throws IOException {
		int port = 8000;
		IOException last = null;
		for (int i = 0; i < 10; i++) {
			try {
				start(port + i);
				last = null;
				break;
			} catch (BindException e) {
				last = e;
				continue;
			}
		}
		if (last != null) {
			throw last;
		}
	}

	private void start(int port) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		server.start();
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	public List<String> getUrls() {
		List<String> result = new ArrayList<>();
		for (String cur : mockedPaths) {
			result.add("http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort() + cur);
		}
		return result;
	}

}
