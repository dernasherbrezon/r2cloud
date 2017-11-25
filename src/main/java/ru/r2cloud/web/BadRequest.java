package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class BadRequest extends ModelAndView {

	private static final long serialVersionUID = 7115412102756236505L;

	public BadRequest() {
		this("Invalid request");
	}

	public BadRequest(String generalError) {
		setData(new ValidationResult(generalError).toJson());
		setStatus(Response.Status.BAD_REQUEST);
	}

}
