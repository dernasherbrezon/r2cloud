package ru.r2cloud.web.controller;

import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.web.HttpContoller;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Home implements HttpContoller {

	private final ADSBDao dao;

	public Home(ADSBDao dao) {
		this.dao = dao;
	}

	@Override
	public ModelAndView httpGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("index.ftl");
		result.put("entity", dao.getLatestMessages());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/";
	}

}
