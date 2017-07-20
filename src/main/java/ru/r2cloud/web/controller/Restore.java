package ru.r2cloud.web.controller;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Restore extends AbstractHttpController {
	
	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return new ModelAndView("restore");
	}

	@Override
	public String getRequestMappingURL() {
		return "/restore";
	}
	
}
