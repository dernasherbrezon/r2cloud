package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MockServer {

	private HttpServer server;
	private final int port;

	public MockServer() {
		this.port = 8000;
	}

	public MockServer(int port) {
		this.port = port;
	}

	public void mockResponse(String url, String data) {
		server.createContext(url, new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (data == null) {
					exchange.sendResponseHeaders(404, 0);
					return;
				}
				exchange.sendResponseHeaders(200, data.length());
				OutputStream os = exchange.getResponseBody();
				os.write(data.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		});
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
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
