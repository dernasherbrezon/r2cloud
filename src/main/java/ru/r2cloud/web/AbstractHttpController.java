package ru.r2cloud.web;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public abstract class AbstractHttpController implements HttpContoller {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return null;
	}
	
	@Override
	public ModelAndView doPost(JsonObject request) {
		return null;
	}
	
}
