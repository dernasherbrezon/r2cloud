package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CelestrakServer {

	private String data;
	private HttpServer server;

	public void mockResponse(String data) {
		this.data = data;
	}

	public void start(int port) throws IOException {
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
