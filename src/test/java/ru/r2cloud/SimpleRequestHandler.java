package ru.r2cloud;

public class SimpleRequestHandler implements RequestHandler {

	private final String response;

	private String request;

	public SimpleRequestHandler(String response) {
		this.response = response;
	}

	@Override
	public String handle(String request) {
		this.request = request;
		return response;
	}

	public String getRequest() {
		return request;
	}

}
