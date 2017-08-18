package ru.r2cloud.web.controller;

import java.util.Map.Entry;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.MimeType;
import ru.r2cloud.web.ModelAndView;

import com.codahale.metrics.health.HealthCheck.Result;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class StatusData extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		result.setType(MimeType.JSON);
		JsonObject entity = Json.object();
		for (Entry<String, Result> cur : Metrics.HEALTH_REGISTRY.runHealthChecks().entrySet()) {
			JsonObject value = Json.object().add("status", cur.getValue().getDetails().get("status").toString());
			if (!cur.getValue().isHealthy()) {
				value.add("message", cur.getValue().getMessage());
			}
			entity.add(cur.getKey(), value);
		}
		result.put("entity", entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/status/data.json";
	}

}
