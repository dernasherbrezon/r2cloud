package ru.r2cloud.web.api.status;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.RotatorStatus;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Overview extends AbstractHttpController {

	private final DeviceManager deviceManager;

	public Overview(DeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = Json.object();
		JsonArray devices = new JsonArray();
		for (DeviceStatus cur : deviceManager.getStatus()) {
			JsonObject curObj = Json.object();
			curObj.add("status", cur.getStatus().name());
			if (cur.getFailureMessage() != null) {
				curObj.add("failureMessage", cur.getFailureMessage());
			}
			if (cur.getModel() != null) {
				curObj.add("model", cur.getModel());
			}
			if (cur.getBatteryLevel() != null) {
				curObj.add("batteryLevel", cur.getBatteryLevel());
			}
			if (cur.getSignalLevel() != null) {
				curObj.add("signalLevel", cur.getSignalLevel());
			}
			if (cur.getDeviceName() != null) {
				curObj.add("connection", cur.getDeviceName());
			}
			curObj.add("minFrequency", cur.getConfig().getMinimumFrequency());
			curObj.add("maxFrequency", cur.getConfig().getMaximumFrequency());
			curObj.add("rotator", createRotatorStatus(cur.getRotatorStatus()));
			devices.add(curObj);
		}
		entity.add("devices", devices);
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		entity.add("serverTime", sdf.format(new Date()));
		result.setData(entity.toString());
		return result;
	}

	private static JsonObject createRotatorStatus(RotatorStatus cur) {
		JsonObject rotator = Json.object();
		rotator.add("status", cur.getStatus().name());
		if (cur.getModel() != null) {
			rotator.add("model", cur.getModel());
		}
		if (cur.getHostport() != null) {
			rotator.add("connection", cur.getHostport());
		}
		if (cur.getFailureMessage() != null) {
			rotator.add("failureMessage", cur.getFailureMessage());
		}
		return rotator;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/status/overview";
	}

}
