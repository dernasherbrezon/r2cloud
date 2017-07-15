package ru.r2cloud.web.controller;

import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.web.HttpContoller;
import ru.r2cloud.web.MimeType;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class ADSBData implements HttpContoller {

	private final ADSBDao dao;

	public ADSBData(ADSBDao dao) {
		this.dao = dao;
	}
	
	@Override
	public ModelAndView httpGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		result.setType(MimeType.JSON);
		result.put("entity", dao.getAirplanes());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/adsb/data.json";
	}

}
