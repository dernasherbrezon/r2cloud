package ru.r2cloud.web.api;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class TLE extends AbstractHttpController {

	private final Configuration config;
	private final SatelliteDao service;

	public TLE(Configuration config, SatelliteDao service) {
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
		for (Satellite cur : service.findAll()) {
			if (cur.getTle() == null) {
				continue;
			}
			JsonObject curTle = new JsonObject();
			curTle.add("id", cur.getId());
			JsonArray curData = new JsonArray();
			for (String curDataEntry : cur.getTle().getRaw()) {
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
