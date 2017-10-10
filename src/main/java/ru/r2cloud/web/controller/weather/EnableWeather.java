package ru.r2cloud.web.controller.weather;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class EnableWeather extends AbstractHttpController {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EnableWeather.class);

	private final Configuration config;

	public EnableWeather(Configuration config) {
		this.config = config;
		Util.initDirectory(new File(config.getProperty("satellites.wxtoimg.license.path")).getParent());
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
		try (BufferedWriter w = new BufferedWriter(new FileWriter(config.getProperty("satellites.wxtoimg.license.path")))) {
			w.append("2.11.2 beta\n");
		} catch (Exception e) {
			LOG.error("unable to write license file", e);
			return new Redirect("/admin/weather?errors=UnknownServerError");
		}
		config.setProperty("satellites.enabled", true);
		config.update();
		return new Redirect("/admin/weather");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/weather/enable";
	}

}
