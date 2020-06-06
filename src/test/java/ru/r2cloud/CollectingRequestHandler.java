package ru.r2cloud;

import java.util.ArrayList;
import java.util.List;

public class CollectingRequestHandler implements RequestHandler {

	private final String response;

	private final List<String> requests = new ArrayList<>();

	public CollectingRequestHandler(String response) {
		this.response = response;
	}

	@Override
	public String handle(String request) {
		if (request != null && request.equals("\\get_info")) {
			return "TEST\n";
		}
		requests.add(request);
		return response;
	}

	public List<String> getRequests() {
		return requests;
	}

}
