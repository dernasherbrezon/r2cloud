package ru.r2cloud.web.controller;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Status extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		long end = System.currentTimeMillis();
		long start = end - 24 * 60 * 60 * 1000; //last 1 day
		ModelAndView result = new ModelAndView("status");
		result.put("start", start);
		result.put("end", end);
		result.put("metrics", Metrics.REGISTRY.getMetrics().keySet());
		return result;
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/admin/status2";
	}
	
}
