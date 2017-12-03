package ru.r2cloud.web.api.setup;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Setup extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return new ModelAndView("setup");
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/setup";
	}
	
}
