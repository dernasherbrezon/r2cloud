package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CelestrakServer {

	private String data;
	private HttpServer server;

	public void mockResponse(String data) {
		this.data = data;
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
		server.createContext("/NORAD/elements/active.txt", new HttpHandler() {

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
		server.createContext("/NORAD/elements/satnogs.txt", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] bytes = "".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, bytes.length);
				OutputStream os = exchange.getResponseBody();
				os.write(bytes);
				os.close();
			}
		});
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	public List<String> getUrls() {
		List<String> result = new ArrayList<>();
		result.add("http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort() + "/NORAD/elements/satnogs.txt");
		result.add("http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort() + "/NORAD/elements/active.txt");
		return result;
	}

	public String getUrlsAsProperty() {
		StringBuilder result = new StringBuilder();
		List<String> urls = getUrls();
		for (int i = 0; i < urls.size(); i++) {
			if (i != 0) {
				result.append(',');
			}
			result.append(urls.get(i));
		}
		return result.toString();
	}

}
