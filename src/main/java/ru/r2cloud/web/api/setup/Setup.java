package ru.r2cloud.web.api.setup;

import java.io.BufferedReader;
import java.io.FileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.AccessToken;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Setup extends AbstractHttpController {

	private final static Logger LOG = LoggerFactory.getLogger(Setup.class);

	private final Authenticator auth;
	private final Configuration config;

	public Setup(Authenticator auth, Configuration config) {
		this.auth = auth;
		this.config = config;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		ValidationResult errors = new ValidationResult();
		
		String username = ((JsonObject) request).getString("username", null);
		String password = ((JsonObject) request).getString("password", null);
		String keyword = ((JsonObject) request).getString("keyword", null);
		
		if (username == null || username.trim().length() == 0) {
			errors.put("username", "Cannot be empty");
		}
		if (password == null || password.trim().length() == 0) {
			errors.put("password", "Cannot be empty");
		}
		if (keyword == null || keyword.trim().length() == 0) {
			errors.put("keyword", "Cannot be empty");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			return new BadRequest(errors);
		}

		// keyword location extracted for dev environment
		try (BufferedReader r = new BufferedReader(new FileReader(config.getProperty("server.keyword.location")))) {
			String actualKeyword = r.readLine();
			// actualKeyword can be null here
			// keyword should not be null here. However eclipse complains about
			// potential npe.
			if (keyword == null || !keyword.equals(actualKeyword)) {
				errors.put("keyword", "Invalid keyword");
			}
		} catch (Exception e) {
			String message = "unable to read r2cloud file. ";
			if (LOG.isDebugEnabled()) {
				LOG.debug(message, e);
			} else {
				LOG.info(message + e.getMessage());
			}
			errors.setGeneral("Unable to read r2cloud file");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			return new BadRequest(errors);
		}

		auth.setPassword(username, password);

		return AccessToken.doLogin(auth, username, password);
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/setup/setup";
	}

}
