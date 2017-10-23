package ru.r2cloud.web.controller;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class LoadTLE extends AbstractHttpController {

	private final Configuration config;
	private final TLEDao service;

	public LoadTLE(Configuration config, TLEDao service) {
		this.config = config;
		this.service = service;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("tle");
		Long lastUpdateMillis = config.getLong("satellites.tle.lastupdateAtMillis");
		long current = System.currentTimeMillis();
		String color;
		if (current - lastUpdateMillis <= TimeUnit.DAYS.toMillis(7)) {
			color = "text-success";
		} else if (current - lastUpdateMillis <= TimeUnit.DAYS.toMillis(30)) {
			color = "text-warning";
		} else {
			color = "text-danger";
		}
		result.put("color", color);
		result.put("lastupdate", new Date(lastUpdateMillis));
		result.put("satellites", service.findAll());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/tle";
	}

}
