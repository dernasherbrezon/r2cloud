package ru.r2cloud.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class WeatherObservation extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherObservation.class);

	private final ObservationResultDao resultDao;

	public WeatherObservation(ObservationResultDao resultDao) {
		this.resultDao = resultDao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String satelliteId = WebServer.getParameter(session, "satelliteId");
		String id = WebServer.getParameter(session, "id");
		if (satelliteId == null || id == null) {
			LOG.info("missing parameters");
			return new BadRequest("missing parameters");
		}
		ObservationFull entity = resultDao.find(satelliteId, id);
		if (entity == null) {
			LOG.info("not found: " + satelliteId + " id: " + id);
			return new NotFound();
		}
		JsonObject json = entity.toJson();
		ModelAndView result = new ModelAndView();
		result.setData(json.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather/observation";
	}

}
