package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class Success extends ModelAndView {
	
	private static final long serialVersionUID = 6849961467035419038L;

	public Success() {
		setData("{}");
		setStatus(Response.Status.OK);
	}

}
