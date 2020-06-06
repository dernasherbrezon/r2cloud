package ru.r2cloud;

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
}
