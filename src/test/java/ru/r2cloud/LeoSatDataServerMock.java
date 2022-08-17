package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LeoSatDataServerMock {

	private HttpServer server;
	private boolean newLaunchConfigured = false;
	private boolean satelliteConfigured = false;

	public void setObservationMock(HttpHandler observationMock) {
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

	public void setDataMock(long id, HttpHandler mock) {
		server.createContext("/api/v1/observation/" + id + "/data", mock);
	}

	public void setSpectogramMock(long id, JsonHttpResponse mock) {
		server.createContext("/api/v1/observation/" + id + "/spectogram", mock);
	}

	public void setSatelliteMock(HttpHandler mock) {
		String path = "/api/v1/satellite";
		if (satelliteConfigured) {
			server.removeContext(path);
		}
		server.createContext(path, mock);
		satelliteConfigured = true;
	}
	
	public void setSatelliteMock(String message, int code) {
		HttpHandler plainHandler = new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(code, message.length());
				OutputStream os = exchange.getResponseBody();
				os.write(message.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		};
		setSatelliteMock(plainHandler);
	}

	public void setNewLaunchMock(HttpHandler mock) {
		String newLaunchPath = "/api/v1/satellite/newlaunch2";
		if (newLaunchConfigured) {
			server.removeContext(newLaunchPath);
		}
		server.createContext(newLaunchPath, mock);
		newLaunchConfigured = true;
	}
	
	public void setNewLaunchMock(String message, int code) {
		HttpHandler plainHandler = new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(code, message.length());
				OutputStream os = exchange.getResponseBody();
				os.write(message.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		};
		setNewLaunchMock(plainHandler);
	}

	public void setNewLaunchMock(JsonObject newLaunch) {
		String message = "[" + newLaunch.toString() + "]";
		setNewLaunchMock(message, 200);
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
