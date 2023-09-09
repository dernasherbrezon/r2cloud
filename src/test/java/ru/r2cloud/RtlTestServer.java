package ru.r2cloud;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RtlTestServer {

	private final int port;
	private String test;
	private HttpServer server;
	
	public RtlTestServer(int port) {
		this.port = port;
	}

	public void mockTest(String test) {
		this.test = test;
	}

	public void mockDefault() {
		mockTest("  0:  Realtek, RTL2838UHIDIR, SN: 00000001\n");
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		server.start();
		server.createContext("/t", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (test == null) {
					exchange.sendResponseHeaders(404, 0);
				} else {
					exchange.sendResponseHeaders(200, 0);
					exchange.getResponseBody().write(test.getBytes(StandardCharsets.UTF_8));
					exchange.getResponseBody().flush();
				}
				exchange.getResponseBody().close();
			}
		});
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}
}
