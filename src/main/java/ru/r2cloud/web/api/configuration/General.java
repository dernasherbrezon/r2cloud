package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.AutoUpdate;
import ru.r2cloud.model.PpmType;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class General extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(General.class);

	private final Configuration config;
	private final AutoUpdate autoUpdate;

	public General(Configuration config, AutoUpdate autoUpdate) {
		this.config = config;
		this.autoUpdate = autoUpdate;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		entity.add("lat", config.getDouble("locaiton.lat"));
		entity.add("lng", config.getDouble("locaiton.lon"));
		entity.add("autoUpdate", autoUpdate.isEnabled());
		entity.add("ppmType", config.getPpmType().toString());
		Integer currentPpm = config.getInteger("ppm.current");
		if (currentPpm != null) {
			entity.add("ppm", currentPpm);
		}
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		Double lat = WebServer.getDouble(request, "lat");
		Double lon = WebServer.getDouble(request, "lng");
		if (lat == null) {
			errors.put("lat", Messages.CANNOT_BE_EMPTY);
		}
		if (lon == null) {
			errors.put("lng", Messages.CANNOT_BE_EMPTY);
		}
		String ppmTypeStr = WebServer.getString(request, "ppmType");
		PpmType ppmType = null;
		if (ppmTypeStr == null) {
			ppmTypeStr = "AUTO";
		} else {
			try {
				ppmType = PpmType.valueOf(ppmTypeStr);
			} catch (Exception e) {
				errors.put("ppmType", "unkown ppmType");
			}
		}

		Integer ppm = null;
		if (ppmType != null && ppmType.equals(PpmType.MANUAL)) {
			try {
				ppm = WebServer.getInteger(request, "ppm");
				if (ppm == null) {
					errors.put("ppm", Messages.CANNOT_BE_EMPTY);
				}
			} catch (NumberFormatException e) {
				if (ppm == null) {
					errors.put("ppm", "not an integer");
				}
			}
		}
		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		autoUpdate.setEnabled(WebServer.getBoolean(request, "autoUpdate"));
		config.setProperty("locaiton.lat", String.valueOf(lat));
		config.setProperty("locaiton.lon", String.valueOf(lon));
		config.setProperty("ppm.calculate.type", ppmTypeStr);
		if (ppm != null) {
			config.setProperty("ppm.current", ppm);
		}
		config.update();
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/general";
	}
}
