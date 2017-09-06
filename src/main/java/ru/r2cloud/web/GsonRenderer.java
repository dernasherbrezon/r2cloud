package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class GsonRenderer {

	public GsonRenderer() {
		// do nothing
	}

	public Response render(ModelAndView model) {
		IStatus status;
		if (model.getStatus() != null) {
			status = model.getStatus();
		} else {
			status = Status.OK;
		}
		return NanoHTTPD.newFixedLengthResponse(status, MimeType.JSON.getType(), (String) model.get("entity"));
	}

}
