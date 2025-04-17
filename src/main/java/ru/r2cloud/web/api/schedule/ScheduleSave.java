package ru.r2cloud.web.api.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ScheduleSave extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleSave.class);

	private final SatelliteDao dao;
	private final DeviceManager deviceManager;

	public ScheduleSave(SatelliteDao dao, DeviceManager deviceManager) {
		this.dao = dao;
		this.deviceManager = deviceManager;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		boolean enabled = WebServer.getBoolean(request, "enabled");
		String id = WebServer.getString(request, "id");
		ValidationResult errors = new ValidationResult();
		if (id == null || id.trim().length() == 0) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}
		Satellite satelliteToEdit = null;
		if (errors.isEmpty()) {
			satelliteToEdit = dao.findById(id);
			if (satelliteToEdit == null) {
				errors.put("id", "Unknown satellite");
			}
		}
		if (!errors.isEmpty() || satelliteToEdit == null) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		satelliteToEdit.setEnabled(enabled);
		dao.update(satelliteToEdit);

		ObservationRequest nextObservation = null;
		if (enabled) {
			nextObservation = deviceManager.enable(satelliteToEdit);
		} else {
			deviceManager.cancelObservations(satelliteToEdit);
		}

		JsonObject entity = new JsonObject();
		entity.add("id", satelliteToEdit.getId());
		entity.add("name", satelliteToEdit.getName());
		entity.add("enabled", satelliteToEdit.isEnabled());
		Transmitter transmitter = null;
		if (nextObservation != null) {
			entity.add("nextPass", nextObservation.getStartTimeMillis());
			transmitter = satelliteToEdit.getById(nextObservation.getTransmitterId());
		}
		if (transmitter == null) {
			transmitter = satelliteToEdit.getFirstOrNull();
		}
		if (transmitter != null) {
			entity.add("frequency", transmitter.getFrequency());
			if (transmitter.getModulation() != null) {
				entity.add("modulation", transmitter.getModulation().toString());
			}
		}
		entity.add("source", satelliteToEdit.getSource().name());
		entity.add("status", satelliteToEdit.getOverallStatus().name());

		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/save";
	}

}
