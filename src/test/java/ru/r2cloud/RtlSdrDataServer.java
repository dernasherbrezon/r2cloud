package ru.r2cloud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.util.Util;

public class RtlSdrDataServer {

	private String filename;
	private HttpServer server;

	public void mockResponse(String filename) {
		this.filename = filename;
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 8002), 0);
		server.start();
		server.createContext("/", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (filename == null) {
					return;
				}
				exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
				try (InputStream is = RtlSdrDataServer.class.getResourceAsStream(filename)) {
					if (is == null) {
						exchange.sendResponseHeaders(404, 0);
						return;
					}
					exchange.sendResponseHeaders(200, 0);
					try (OutputStream os = exchange.getResponseBody();) {
						Util.copy(is, os);
					}
				}
			}
		});
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}
}
