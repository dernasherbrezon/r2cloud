package ru.r2cloud.web.api.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class ScheduleSave extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleSave.class);

	private final SatelliteDao dao;
	private final Scheduler scheduler;

	public ScheduleSave(SatelliteDao dao, Scheduler scheduler) {
		this.dao = dao;
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		boolean enabled = WebServer.getBoolean(request, "enabled");
		String id = WebServer.getString(request, "id");
		ValidationResult errors = new ValidationResult();
		if (id == null || id.trim().length() == 0) {
			errors.put("id", "Cannot be empty");
		}
		Satellite satelliteToEdit = null;
		if (errors.isEmpty()) {
			satelliteToEdit = dao.findById(id);
			if (satelliteToEdit == null) {
				errors.put("id", "Unknown satellite");
			}
		}
		if (!errors.isEmpty() || satelliteToEdit == null) {
			LOG.info("unable to save: " + errors);
			return new BadRequest(errors);
		}

		satelliteToEdit.setEnabled(enabled);
		dao.update(satelliteToEdit);

		JsonObject entity = new JsonObject();
		entity.add("id", satelliteToEdit.getId());
		entity.add("name", satelliteToEdit.getName());
		entity.add("enabled", satelliteToEdit.isEnabled());
		entity.add("frequency", satelliteToEdit.getFrequency());
		Long nextObservation = scheduler.getNextObservation(satelliteToEdit.getId());
		if (nextObservation != null) {
			entity.add("nextPass", nextObservation);
		}

		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/save";
	}

}
