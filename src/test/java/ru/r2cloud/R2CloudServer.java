package ru.r2cloud;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class R2CloudServer {

	private HttpServer server;

	public void setObservationMock(JsonHttpResponse observationMock) {
		server.createContext("/api/v1/observation", observationMock);
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
