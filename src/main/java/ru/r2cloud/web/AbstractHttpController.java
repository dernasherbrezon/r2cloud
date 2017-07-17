package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public abstract class AbstractHttpController implements HttpContoller {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return null;
	}
	
	@Override
	public ModelAndView doPost(IHTTPSession session) {
		return null;
	}
	
}
