package ru.r2cloud;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class R2CloudServer {

	private HttpServer server;

	public void setObservationMock(JsonHttpResponse observationMock) {
		server.createContext("/api/v1/observation", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				String requestUri = exchange.getRequestURI().toString();
				if (requestUri.endsWith("/observation")) {
					observationMock.handle(exchange);
				}
			}
		});
	}

	public void setMetricsMock(JsonHttpResponse metricsMock) {
		server.createContext("/api/v1/metrics", metricsMock);
	}

	public void setDataMock(long id, JsonHttpResponse mock) {
		server.createContext("/api/v1/observation/" + id + "/data", mock);
	}

	public void setSpectogramMock(long id, JsonHttpResponse mock) {
		server.createContext("/api/v1/observation/" + id + "/spectogram", mock);
	}

	public void setNewLaunchMock(JsonHttpResponse mock) {
		server.createContext("/api/v1/satellite/newlaunch", mock);
	}

	public void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
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
