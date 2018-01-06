package ru.r2cloud.web.api;

import java.util.Map.Entry;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.tle.TLEDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class TLE extends AbstractHttpController {

	private final Configuration config;
	private final TLEDao service;

	public TLE(Configuration config, TLEDao service) {
		this.config = config;
		this.service = service;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		Long lastUpdateMillis = config.getLong("satellites.tle.lastupdateAtMillis");
		if (lastUpdateMillis != null) {
			entity.add("lastUpdated", lastUpdateMillis);
		}
		JsonArray tle = new JsonArray();
		for (Entry<String, ru.r2cloud.model.TLE> cur : service.findAll().entrySet()) {
			JsonObject curTle = new JsonObject();
			curTle.add("id", cur.getKey());
			JsonArray curData = new JsonArray();
			for (String curDataEntry : cur.getValue().getRaw()) {
				curData.add(curDataEntry);
			}
			curTle.add("data", curData);
			tle.add(curTle);
		}
		entity.add("tle", tle);
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/tle";
	}

}
