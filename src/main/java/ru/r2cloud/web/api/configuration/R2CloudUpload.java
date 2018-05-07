package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.cloud.R2CloudService;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.WebServer;

public class R2CloudUpload extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudUpload.class);

	private final R2CloudService service;

	public R2CloudUpload(R2CloudService service) {
		this.service = service;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String satelliteId = ((JsonObject) request).getString("satelliteId", null);
		String observationId = ((JsonObject) request).getString("observationId", null);
		if (satelliteId == null || observationId == null) {
			LOG.info("missing parameters");
			return new BadRequest("missing parameters");
		}
		
		service.uploadObservation(satelliteId, observationId);
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather/observation/upload";
	}

}
