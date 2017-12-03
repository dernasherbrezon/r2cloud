package ru.r2cloud.web.api.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class Restore extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(Restore.class);

	private final Authenticator auth;

	public Restore(Authenticator auth) {
		this.auth = auth;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		ValidationResult errors = new ValidationResult();
		String username = ((JsonObject) request).getString("username", null);
		if (username == null || username.trim().length() == 0) {
			errors.put("username", "Cannot be empty");
		}
		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			return new BadRequest(errors);
		}
		auth.resetPassword(username);
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/setup/restore";
	}

}
