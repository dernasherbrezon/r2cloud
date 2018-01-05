package ru.r2cloud.web.api;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;

public class Health extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return new Success();
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/api/v1/health";
	}
	
}
