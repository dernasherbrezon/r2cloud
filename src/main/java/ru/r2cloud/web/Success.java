package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.Response;

public class Success extends ModelAndView {
	
	public Success() {
		setData("{}");
		setStatus(Response.Status.OK);
	}

}
