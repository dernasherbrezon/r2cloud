package ru.r2cloud.web.api.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ScheduleStart extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleStart.class);

	private final SatelliteDao satelliteDao;
	private final Scheduler scheduler;

	public ScheduleStart(SatelliteDao satelliteDao, Scheduler scheduler) {
		this.satelliteDao = satelliteDao;
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String id = WebServer.getString(request, "id");
		ValidationResult errors = new ValidationResult();
		if (id == null || id.trim().length() == 0) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}
		Satellite satellite = null;
		if (errors.isEmpty()) {
			satellite = satelliteDao.findById(id);
			if (satellite == null) {
				errors.put("id", "Unknown satellite");
			}
		}
		if (!errors.isEmpty() || satellite == null) {
			LOG.info("unable to schedule: {}", errors);
			return new BadRequest(errors);
		}

		ObservationRequest req = scheduler.startImmediately(satellite);
		if (req == null) {
			return new InternalServerError();
		}
		
		JsonObject entity = new JsonObject();
		entity.add("id", req.getId());
		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/immediately/start";
	}

}
