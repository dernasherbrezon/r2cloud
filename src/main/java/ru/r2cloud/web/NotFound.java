package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class NotFound extends ModelAndView {

	public NotFound() {
		setData("{}");
		setStatus(Response.Status.NOT_FOUND);
	}

}
