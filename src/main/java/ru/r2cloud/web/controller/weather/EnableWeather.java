package ru.r2cloud.web.controller.weather;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class EnableWeather extends AbstractHttpController {

	private final Configuration config;

	public EnableWeather(Configuration config) {
		this.config = config;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		Boolean agreeToC = WebServer.getBoolean(session, "agreeToC");
		if (agreeToC == null || !agreeToC) {
			Redirect result;
			try {
				result = new Redirect("/admin/weather?errors=" + URLEncoder.encode("You must agree with Terms and conditions", "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				result = new Redirect("/admin/weather?errors=UnknownServerError");
			}
			return result;
		}
		config.setProperty("satellites.enabled", "true");
		config.update();
		return new Redirect("/admin/weather");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/weather/enable";
	}

}
