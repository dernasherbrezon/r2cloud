package ru.r2cloud.web.api.schedule;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class SatelliteLoad extends AbstractHttpController {

	private final SatelliteDao dao;

	public SatelliteLoad(SatelliteDao dao) {
		this.dao = dao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String id = WebServer.getParameter(session, "id");
		Satellite satellite = dao.findById(id);
		if (satellite == null || satellite.getTle() == null) {
			return new NotFound();
		}
		JsonObject json = satellite.toJson();
		json.add("tle", satellite.getTle().toJson());
		json.add("source", satellite.getSource().name());
		ModelAndView result = new ModelAndView();
		result.setData(json);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/satellite/load";
	}
}
