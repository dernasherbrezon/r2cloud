package ru.r2cloud;

import java.util.ArrayList;
import java.util.List;

public class SequentialRequestHandler implements RequestHandler {

	private final RequestHandler[] handlers;
	private int current = 0;

	public SequentialRequestHandler(RequestHandler... handlers) {
		this.handlers = handlers;
	}

	@Override
	public String handle(String request) {
		String result = handlers[current].handle(request);
		current++;
		return result;
	}

	public List<String> getRequests() {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < handlers.length; i++) {
			if (handlers[i] instanceof SimpleRequestHandler) {
				SimpleRequestHandler cur = (SimpleRequestHandler) handlers[i];
				result.add(cur.getRequest());
			}
			if (handlers[i] instanceof SequentialRequestHandler) {
				SequentialRequestHandler cur = (SequentialRequestHandler) handlers[i];
				result.addAll(cur.getRequests());
			}
		}
		return result;
	}
}
