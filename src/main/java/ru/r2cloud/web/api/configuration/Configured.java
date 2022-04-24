package ru.r2cloud.web.api.configuration;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.ModelAndView;

public class Configured extends AbstractHttpController {

	private final Authenticator auth;
	private final Configuration config;

	public Configured(Authenticator auth, Configuration config) {
		this.auth = auth;
		this.config = config;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		JsonObject entity = new JsonObject();
		entity.add("configured", !auth.isFirstStart());
		entity.add("generalSetup", isGenerallyConfigured());
		entity.add("presentationMode", config.getBoolean("presentationMode"));
		
		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}
	
	private boolean isGenerallyConfigured() {
		return config.getProperty("locaiton.lat") != null && config.getProperty("locaiton.lon") != null;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/configured";
	}

}
