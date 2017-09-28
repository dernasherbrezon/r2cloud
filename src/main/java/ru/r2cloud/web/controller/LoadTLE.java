package ru.r2cloud.web.controller;

import java.util.Date;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;


public class LoadTLE extends AbstractHttpController {
	
	private final Configuration config;
	
	public LoadTLE(Configuration config) {
		this.config = config;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("tle");
		result.put("lastupdate", new Date(config.getLong("satellites.tle.lastupdateAtMillis")));
//		result.put("entity", ConfigurationBean.fromConfig(props));
		return result;		
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/admin/tle";
	}
	
	

}
