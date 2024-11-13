package ru.r2cloud.web.api.device;

import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Clock;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class DeviceSchedule extends AbstractHttpController {

	private final DeviceManager deviceManager;
	private final SatelliteDao satelliteDao;
	private final Clock clock;

	public DeviceSchedule(DeviceManager deviceManager, SatelliteDao satelliteDao, Clock clock) {
		this.deviceManager = deviceManager;
		this.satelliteDao = satelliteDao;
		this.clock = clock;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String id = WebServer.getParameter(session, "id");
		Device device = deviceManager.findDeviceById(id);
		if (device == null) {
			return new NotFound();
		}
		JsonArray result = new JsonArray();
		List<ObservationRequest> request = device.findScheduledObservations();
		Collections.sort(request, ObservationRequestComparator.INSTANCE);
		long currentTime = clock.millis();
		for (ObservationRequest cur : request) {
			if (cur.getEndTimeMillis() < currentTime) {
				continue;
			}
			Satellite satellite = satelliteDao.findById(cur.getSatelliteId());
			if (satellite == null) {
				continue;
			}
			JsonObject curRequest = new JsonObject();
			curRequest.set("id", cur.getId());
			curRequest.set("start", cur.getStartTimeMillis());
			curRequest.set("end", cur.getEndTimeMillis());
			curRequest.set("satelliteId", cur.getSatelliteId());
			curRequest.set("name", satellite.getName());
			result.add(curRequest);
		}
		ModelAndView model = new ModelAndView();
		model.setData(result);
		return model;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/schedule";
	}

}
