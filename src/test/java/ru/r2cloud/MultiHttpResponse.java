package ru.r2cloud;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MultiHttpResponse implements HttpHandler {

	private final HttpHandler[] handlers;
	private int currentIndex = 0;

	public MultiHttpResponse(HttpHandler... handlers) {
		this.handlers = handlers;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (currentIndex >= handlers.length) {
			throw new IOException("unexpected request");
		}
		HttpHandler cur = handlers[currentIndex];
		cur.handle(exchange);
		currentIndex++;
	}

}
