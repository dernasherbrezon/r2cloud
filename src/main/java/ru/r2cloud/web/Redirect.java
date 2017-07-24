package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class Redirect extends ModelAndView {

	private static final long serialVersionUID = -1878622409174673786L;

	public Redirect(String location) {
		addHeader("Location", location);
		setStatus(Response.Status.TEMPORARY_REDIRECT);
	}

}
