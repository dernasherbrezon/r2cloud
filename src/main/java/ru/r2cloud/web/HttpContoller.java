package ru.r2cloud.web;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public interface HttpContoller {

	ModelAndView httpGet(IHTTPSession session);
	
	String getRequestMappingURL();
	
}
