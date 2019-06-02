package ru.r2cloud;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RtlTestServer {

	private String test;
	private String ppm;
	private HttpServer server;

	public void mockTest(String test) {
		this.test = test;
	}

	public void mockPpm(String ppm) {
		this.ppm = ppm;
	}

	public void mockDefault() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			builder.append("real sample rate: 2048118 current PPM: 58 cumulative PPM: 53\n");
		}
		mockPpm(builder.toString());
		mockTest("  0:  Realtek, RTL2838UHIDIR, SN: 00000001\n");
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 8003), 0);
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
		server.createContext("/ppm", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if (ppm == null) {
					exchange.sendResponseHeaders(404, 0);
				} else {
					exchange.sendResponseHeaders(200, 0);
					exchange.getResponseBody().write(ppm.getBytes(StandardCharsets.UTF_8));
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
