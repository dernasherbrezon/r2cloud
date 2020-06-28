package ru.r2cloud.web.api.status;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map.Entry;

import com.codahale.metrics.health.HealthCheck.Result;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.metrics.Status;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Overview extends AbstractHttpController {

	private final Metrics metrics;

	public Overview(Metrics metrics) {
		this.metrics = metrics;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = Json.object();
		for (Entry<String, Result> cur : metrics.getHealthRegistry().runHealthChecks().entrySet()) {
			JsonObject value = Json.object().add("status", cur.getValue().getDetails().get("status").toString());
			if (!cur.getValue().isHealthy()) {
				value.add("message", cur.getValue().getMessage());
			}
			entity.add(cur.getKey(), value);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		JsonObject value = Json.object().add("status", Status.SUCCESS.toString());
		value.add("message", "Server time: " + sdf.format(new Date()));
		entity.add("serverTime", value);
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/status/overview";
	}

}
