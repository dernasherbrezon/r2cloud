package ru.r2cloud.web.api.device;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class DeviceConfigList extends AbstractHttpController {

	private final DeviceManager manager;

	public DeviceConfigList(DeviceManager manager) {
		this.manager = manager;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		JsonArray result = new JsonArray();
		for (DeviceStatus cur : manager.getStatus()) {
			JsonObject curConfig = new JsonObject();
			curConfig.add("id", cur.getConfig().getId());
			curConfig.add("name", cur.getConfig().getName());
			curConfig.add("status", cur.getStatus().name());
			if (cur.getFailureMessage() != null) {
				curConfig.add("failureMessage", cur.getFailureMessage());
			}
			result.add(curConfig);
		}
		ModelAndView model = new ModelAndView();
		model.setData(result);
		return model;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/list";
	}
}
