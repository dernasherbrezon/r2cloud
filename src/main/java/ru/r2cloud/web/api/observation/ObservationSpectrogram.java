package ru.r2cloud.web.api.observation;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.SpectogramService;
import ru.r2cloud.model.Observation;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ObservationSpectrogram extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationSpectrogram.class);

	private final ObservationDao dao;
	private final SpectogramService spectogramService;
	private final SignedURL signed;

	public ObservationSpectrogram(ObservationDao dao, SpectogramService spectogramService, SignedURL signed) {
		this.dao = dao;
		this.spectogramService = spectogramService;
		this.signed = signed;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		String id = WebServer.getString(request, "id");
		if (id == null) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}
		String satelliteId = WebServer.getString(request, "satelliteId");
		if (satelliteId == null) {
			errors.put("satelliteId", Messages.CANNOT_BE_EMPTY);
		}

		if (!errors.isEmpty()) {
			return new BadRequest(errors);
		}

		Observation observation = dao.find(satelliteId, id);
		if (observation == null) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}

		if (!observation.hasRawFile()) {
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
		entity.add("spectogramURL", signed.sign(observation.getSpectogramURL()));
		ModelAndView result = new ModelAndView();
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/spectogram";
	}
}
