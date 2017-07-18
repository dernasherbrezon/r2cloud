package ru.r2cloud.web.controller;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class DoLogin extends AbstractHttpController {

	private final Authenticator auth;

	public DoLogin(Authenticator auth) {
		this.auth = auth;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		String username = WebServer.getParameter(session, "j_username");
		String cookie = auth.authenticate(username, WebServer.getParameter(session, "j_password"));
		ModelAndView result = new ModelAndView();
		if (cookie == null) {
			result.setView("login");
			result.put("errors", new ValidationResult("Invalid login or password"));
			result.put("username", username);
			return result;
		}
		result.addHeader("Set-Cookie", "JSESSIONID=" + cookie + "; Max-Age=" + auth.getMaxAgeMillis() / 1000);
		result.addHeader("Location", "/admin/");
		result.setStatus(Response.Status.REDIRECT);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/dologin";
	}

}
