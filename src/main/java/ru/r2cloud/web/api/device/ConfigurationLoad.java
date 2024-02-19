package ru.r2cloud.web.api.device;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class ConfigurationLoad extends AbstractHttpController {

	private final DeviceManager deviceManager;

	public ConfigurationLoad(DeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String id = WebServer.getParameter(session, "id");
		Device device = deviceManager.findDeviceById(id);
		if (device == null) {
			return new NotFound();
		}
		JsonObject json = device.getDeviceConfiguration().toJson();
		ModelAndView result = new ModelAndView();
		result.setData(json.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/load";
	}
}
