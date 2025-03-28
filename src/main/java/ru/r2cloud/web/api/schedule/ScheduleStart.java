package ru.r2cloud.web.api.schedule;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
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
	private final DeviceManager deviceManager;

	public ScheduleStart(SatelliteDao satelliteDao, DeviceManager deviceManager) {
		this.satelliteDao = satelliteDao;
		this.deviceManager = deviceManager;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
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

		LOG.info("start observation for satellite {}", id);

		List<ObservationRequest> req = deviceManager.startImmediately(satellite);
		if (req == null) {
			return new InternalServerError();
		}
		JsonArray ids = new JsonArray();
		for (ObservationRequest cur : req) {
			ids.add(cur.getId());
		}

		JsonObject entity = new JsonObject();
		entity.add("ids", ids);
		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/immediately/start";
	}

}
