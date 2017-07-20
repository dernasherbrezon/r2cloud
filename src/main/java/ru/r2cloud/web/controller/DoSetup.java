package ru.r2cloud.web.controller;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class DoSetup extends AbstractHttpController {

	private final Authenticator auth;

	public DoSetup(Authenticator auth) {
		this.auth = auth;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		String username = WebServer.getParameter(session, "j_username");
		String password = WebServer.getParameter(session, "j_password");
		ValidationResult errors = new ValidationResult();
		if (username == null || username.trim().length() == 0) {
			errors.put("j_username", "Email cannot be empty");
		}
		if (password == null || password.trim().length() == 0) {
			errors.put("j_password", "Password cannot be empty");
		}

		if (!errors.isEmpty()) {
			ModelAndView result = new ModelAndView("setup");
			result.put("errors", errors);
			result.put("username", username);
			return result;
		}

		auth.setPassword(username, password);

		return DoLogin.doLogin(auth, username, password);
	}

	@Override
	public String getRequestMappingURL() {
		return "/doSetup";
	}

}
