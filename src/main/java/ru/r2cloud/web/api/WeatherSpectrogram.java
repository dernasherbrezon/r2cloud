package ru.r2cloud.web.api;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class WeatherSpectrogram extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherSpectrogram.class);

	private final ObservationResultDao dao;
	private final SpectogramService spectogramService;

	public WeatherSpectrogram(ObservationResultDao dao, SpectogramService spectogramService) {
		this.dao = dao;
		this.spectogramService = spectogramService;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String satelliteId = ((JsonObject) request).getString("satelliteId", null);
		String id = ((JsonObject) request).getString("id", null);
		if (id == null || satelliteId == null) {
			LOG.info("missing parameters");
			return new BadRequest("missing parameters");
		}
		ObservationResult observation = dao.find(satelliteId, id);
		if (observation == null) {
			LOG.info("not found: " + satelliteId + " id: " + id);
			return new NotFound();
		}

		if (observation.getWavPath() == null || !observation.getWavPath().exists()) {
			LOG.info("wav file not found");
			return new NotFound();
		}

		File spectogram = spectogramService.create(observation.getWavPath());
		if (spectogram == null) {
			return new InternalServerError();
		}

		if (!dao.saveSpectogram(satelliteId, id, spectogram)) {
			LOG.info("unable to save spectogram");
			return new InternalServerError();
		}

		observation = dao.find(satelliteId, id);
		JsonObject entity = new JsonObject();
		entity.add("spectogramURL", observation.getSpectogramURL());
		ModelAndView result = new ModelAndView();
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather/spectogram";
	}
}
