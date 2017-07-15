package ru.r2cloud.web;

import java.util.Map;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class GsonRenderer {

	private final Gson gson = new Gson();

	public GsonRenderer() {
		// do nothing
	}

	public Response render(Map<String, Object> model) {
		String json = gson.toJson(model.get("entity"));
		return NanoHTTPD.newFixedLengthResponse(Status.OK, MimeType.JSON.getType(), json);
	}

}
