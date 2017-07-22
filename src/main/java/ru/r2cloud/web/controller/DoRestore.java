package ru.r2cloud.web.controller;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class DoRestore extends AbstractHttpController {
	
	private final Authenticator auth;
	
	public DoRestore(Authenticator auth) {
		this.auth = auth;
	}
	
	@Override
	public ModelAndView doPost(IHTTPSession session) {
		String username = WebServer.getParameter(session, "j_username");
		auth.resetPassword(username);
		return new Redirect("/setup");
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/doRestore";
	}

}
