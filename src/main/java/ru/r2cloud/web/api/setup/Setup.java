package ru.r2cloud.web.api.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.AccessToken;
import ru.r2cloud.web.api.Messages;

public class Setup extends AbstractHttpController {

	private static final String PASSWORD_PARAMETER = "password";
	private static final String USERNAME_PARAMETER = "username";
	private static final String KEYWORD_PARAMETER = "keyword";

	private static final Logger LOG = LoggerFactory.getLogger(Setup.class);

	private final Authenticator auth;
	private final Configuration config;

	public Setup(Authenticator auth, Configuration config) {
		this.auth = auth;
		this.config = config;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		String username = WebServer.getString(request, USERNAME_PARAMETER);
		String password = WebServer.getString(request, PASSWORD_PARAMETER);
		String keyword = WebServer.getString(request, KEYWORD_PARAMETER);

		if (username == null || username.trim().length() == 0) {
			errors.put(USERNAME_PARAMETER, Messages.CANNOT_BE_EMPTY);
		}
		if (password == null || password.trim().length() == 0) {
			errors.put(PASSWORD_PARAMETER, Messages.CANNOT_BE_EMPTY);
		}
		if (keyword == null || keyword.trim().length() == 0) {
			errors.put(KEYWORD_PARAMETER, Messages.CANNOT_BE_EMPTY);
		}

		File keywordFile = getFile();
		if (keywordFile == null) {
			errors.setGeneral("Unable to read r2cloud file");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		// keyword location extracted for dev environment
		try (BufferedReader r = new BufferedReader(new FileReader(keywordFile))) {
			String actualKeyword = r.readLine();
			// actualKeyword can be null here
			// keyword should not be null here. However eclipse complains about
			// potential npe.
			if (keyword == null || !keyword.equals(actualKeyword)) {
				errors.put(KEYWORD_PARAMETER, "Invalid keyword");
			}
		} catch (Exception e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("unable to read r2cloud file", e);
			} else {
				LOG.info("unable to read r2cloud file {}", e.getMessage());
			}
			errors.setGeneral("Unable to read r2cloud file");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		auth.setPassword(username, password);

		return AccessToken.doLogin(auth, username, password);
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/setup/setup";
	}

	private File getFile() {
		for (String cur : config.getProperties("server.keyword.location")) {
			File result = new File(cur);
			if (result.exists()) {
				return result;
			}
		}
		return null;
	}

}
