package ru.r2cloud;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class JsonHttpResponse implements HttpHandler {

	private final String responseBody;
	private final int statusCode;

	private String requestContentType;
	private String request;

	public JsonHttpResponse(String name, int statusCode) {
		this.responseBody = Util.loadExpected(name);
		this.statusCode = statusCode;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		requestContentType = exchange.getRequestHeaders().getFirst("Content-Type");
		request = Util.convert(exchange.getRequestBody());
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(statusCode, responseBody.length());
		OutputStream os = exchange.getResponseBody();
		os.write(responseBody.getBytes(StandardCharsets.UTF_8));
		os.close();
	}
	
	public String getRequest() {
		return request;
	}
	
	public String getRequestContentType() {
		return requestContentType;
	}

}
