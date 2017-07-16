package ru.r2cloud.web;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class GsonRenderer {

	public GsonRenderer() {
		// do nothing
	}

	public Response render(Map<String, Object> model) {
		return NanoHTTPD.newFixedLengthResponse(Status.OK, MimeType.JSON.getType(), (String)model.get("entity"));
	}

}
