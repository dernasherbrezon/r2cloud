package ru.r2cloud.web.controller;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Login extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return new ModelAndView("login");
	}

	@Override
	public String getRequestMappingURL() {
		return "/";
	}

}
