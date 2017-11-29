package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class BadRequest extends ModelAndView {

	private static final long serialVersionUID = 7115412102756236505L;

	public BadRequest() {
		this("Invalid request");
	}

	public BadRequest(String generalError) {
		this(new ValidationResult(generalError));
	}

	public BadRequest(ValidationResult validationResult) {
		setData(validationResult.toJson());
		setStatus(Response.Status.BAD_REQUEST);
	}

}
