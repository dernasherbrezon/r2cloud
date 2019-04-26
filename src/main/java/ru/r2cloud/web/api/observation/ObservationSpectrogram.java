package ru.r2cloud.web.api.observation;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class ObservationSpectrogram extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationSpectrogram.class);

	private final ObservationResultDao dao;
	private final SpectogramService spectogramService;
	private final SignedURL signed;

	public ObservationSpectrogram(ObservationResultDao dao, SpectogramService spectogramService, SignedURL signed) {
		this.dao = dao;
		this.spectogramService = spectogramService;
		this.signed = signed;
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
		ObservationFull observation = dao.find(satelliteId, id);
		if (observation == null) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}

		if (!observation.getResult().hasRawFile()) {
			LOG.info("data file not found");
			return new NotFound();
		}

		File spectogram = spectogramService.create(observation);
		if (spectogram == null) {
			return new InternalServerError();
		}

		if (!dao.saveSpectogram(satelliteId, id, spectogram)) {
			LOG.info("unable to save spectogram");
			return new InternalServerError();
		}

		observation = dao.find(satelliteId, id);
		JsonObject entity = new JsonObject();
		entity.add("spectogramURL", signed.sign(observation.getResult().getSpectogramURL()));
		ModelAndView result = new ModelAndView();
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/spectogram";
	}
}
