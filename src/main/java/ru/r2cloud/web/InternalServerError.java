package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class InternalServerError extends ModelAndView {

	public InternalServerError() {
		setStatus(Response.Status.INTERNAL_ERROR);
	}

}
