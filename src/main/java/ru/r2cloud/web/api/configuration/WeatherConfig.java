package ru.r2cloud.web.api.configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class WeatherConfig extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherConfig.class);

	private final Configuration config;

	public WeatherConfig(Configuration config) {
		this.config = config;
		Util.initDirectory(new File(config.getProperty("satellites.wxtoimg.license.path")).getParent());
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}

		boolean agreeWithToC = WebServer.getBoolean(request, "agreeWithToC");
		ValidationResult errors = new ValidationResult();
		if (!agreeWithToC) {
			errors.put("agreeWithToC", "You must agree with ToC");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			return new BadRequest(errors);
		}
		
		LOG.info("enable weather satellites");

		try (BufferedWriter w = new BufferedWriter(new FileWriter(config.getProperty("satellites.wxtoimg.license.path")))) {
			w.append("2.11.2 beta\n");
		} catch (Exception e) {
			LOG.error("unable to write license file", e);
			return new InternalServerError();
		}
		config.setProperty("satellites.enabled", true);
		config.update();

		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/weather";
	}
}