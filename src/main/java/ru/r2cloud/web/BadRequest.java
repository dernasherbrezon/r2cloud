package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class BadRequest extends ModelAndView {

	public BadRequest(String generalError) {
		this(new ValidationResult(generalError));
	}

	public BadRequest(ValidationResult validationResult) {
		setData(validationResult.toJson());
		setStatus(Response.Status.BAD_REQUEST);
	}

}
