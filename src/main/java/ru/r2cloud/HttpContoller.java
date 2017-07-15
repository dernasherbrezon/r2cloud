package ru.r2cloud;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public interface HttpContoller {

	ModelAndView httpGet(IHTTPSession session);
	
	String getRequestMappingURL();
	
}
