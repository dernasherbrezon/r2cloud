package ru.r2cloud.web.controller;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class ADSB extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		return new ModelAndView("adsb");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/adsb";
	}

}
