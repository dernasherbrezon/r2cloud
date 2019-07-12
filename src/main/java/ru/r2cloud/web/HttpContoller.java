package ru.r2cloud.web;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public interface HttpContoller {

	ModelAndView doGet(IHTTPSession session);
	
	String getRequestMappingURL();

	ModelAndView doPost(JsonObject request);
	
}
