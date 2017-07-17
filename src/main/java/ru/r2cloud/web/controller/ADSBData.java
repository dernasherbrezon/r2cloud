package ru.r2cloud.web.controller;

import org.opensky.libadsb.Position;

import ru.r2cloud.model.Airplane;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.MimeType;
import ru.r2cloud.web.ModelAndView;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class ADSBData extends AbstractHttpController {

	private final ADSBDao dao;

	public ADSBData(ADSBDao dao) {
		this.dao = dao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		result.setType(MimeType.JSON);
		JsonArray array = (JsonArray) Json.array();
		for (Airplane cur : dao.getAirplanes()) {
			JsonArray positions = (JsonArray) Json.array();
			if (cur.getPositions() != null) {
				for (Position curPosition : cur.getPositions()) {
					positions.add(Json.object().add("longitude", curPosition.getLongitude()).add("latitude", curPosition.getLatitude()).add("altitude", curPosition.getAltitude()));
				}
			}
			array.add(Json.object().add("icao24", cur.getIcao24()).add("positions", positions));
		}
		result.put("entity", array.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/adsb/data.json";
	}

}
