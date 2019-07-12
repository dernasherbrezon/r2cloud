package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class R2CloudSave extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudSave.class);

	private final Configuration config;

	public R2CloudSave(Configuration config) {
		this.config = config;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		entity.add("apiKey", config.getProperty("r2cloud.apiKey"));
		entity.add("syncSpectogram", config.getBoolean("r2cloud.syncSpectogram"));
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		String apiKey = WebServer.getString(request, "apiKey");
		boolean syncSpectogram = WebServer.getBoolean(request, "syncSpectogram");
		if (apiKey == null || apiKey.trim().length() == 0) {
			errors.put("apiKey", "Cannot be empty");
		}
		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		config.setProperty("r2cloud.apiKey", apiKey);
		config.setProperty("r2cloud.syncSpectogram", String.valueOf(syncSpectogram));
		config.update();
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/r2cloud";
	}

}
