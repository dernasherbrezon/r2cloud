package ru.r2cloud.web.api;

import org.opensky.libadsb.Position;

import ru.r2cloud.model.Airplane;
import ru.r2cloud.rx.ADSBDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class ADSB extends AbstractHttpController {

	private final ADSBDao dao;

	public ADSB(ADSBDao dao) {
		this.dao = dao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonArray array = (JsonArray) Json.array();
		for (Airplane cur : dao.getAirplanes()) {
			JsonArray positions = (JsonArray) Json.array();
			if (cur.getPositions() != null) {
				for (Position curPosition : cur.getPositions()) {
					positions.add(Json.object().add("lng", curPosition.getLongitude()).add("lat", curPosition.getLatitude()));
				}
			}
			array.add(Json.object().add("icao24", cur.getIcao24()).add("positions", positions));
		}
		result.setData(array.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/adsb";
	}

}
