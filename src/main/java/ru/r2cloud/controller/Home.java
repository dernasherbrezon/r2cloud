package ru.r2cloud.controller;

import ru.r2cloud.HttpContoller;
import ru.r2cloud.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Home implements HttpContoller {

	@Override
	public ModelAndView httpGet(IHTTPSession session) {
		return new ModelAndView("index.ftl");
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/";
	}

}
