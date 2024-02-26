package ru.r2cloud.web.api.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.api.Messages;

public class DeviceConfigDelete extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceConfigDelete.class);
	private final DeviceManager manager;
	private final Configuration config;

	public DeviceConfigDelete(Configuration config, DeviceManager manager) {
		this.config = config;
		this.manager = manager;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		JsonValue idsValue = request.get("ids");
		if (idsValue == null || !idsValue.isArray()) {
			ValidationResult errors = new ValidationResult();
			errors.put("ids", Messages.CANNOT_BE_EMPTY);
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		JsonArray ids = idsValue.asArray();
		for (int i = 0; i < ids.size(); i++) {
			JsonValue cur = ids.get(i);
			if (!cur.isString()) {
				continue;
			}
			Device device = manager.findDeviceById(cur.asString());
			if (device == null) {
				continue;
			}
			config.removeDeviceConfiguration(device.getDeviceConfiguration());
		}
		config.update();
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/delete";
	}
}
