package ru.r2cloud.web.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class DoSetup extends AbstractHttpController {

	private final static Logger LOG = Logger.getLogger(DoSetup.class.getName());

	private final Authenticator auth;
	private final Configuration config;

	public DoSetup(Authenticator auth, Configuration config) {
		this.auth = auth;
		this.config = config;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		String username = WebServer.getParameter(session, "j_username");
		String password = WebServer.getParameter(session, "j_password");
		String keyword = WebServer.getParameter(session, "keyword");
		ValidationResult errors = new ValidationResult();
		if (username == null || username.trim().length() == 0) {
			errors.put("j_username", "Email cannot be empty");
		}
		if (password == null || password.trim().length() == 0) {
			errors.put("j_password", "Password cannot be empty");
		}
		if (keyword == null || keyword.trim().length() == 0) {
			errors.put("keyword", "Keyword cannot be empty");
		}

		if (!errors.isEmpty()) {
			return returnErrors(username, errors);
		}

		//keyword location extracted for dev environment
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
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE, message, e);
			} else {
				LOG.info(message + e.getMessage());
			}
			errors.setGeneral("Unable to read r2cloud file");
		}

		if (!errors.isEmpty()) {
			return returnErrors(username, errors);
		}

		auth.setPassword(username, password);

		return DoLogin.doLogin(auth, username, password);
	}

	private static ModelAndView returnErrors(String username, ValidationResult errors) {
		ModelAndView result = new ModelAndView("setup");
		result.put("errors", errors);
		result.put("username", username);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/doSetup";
	}

}
