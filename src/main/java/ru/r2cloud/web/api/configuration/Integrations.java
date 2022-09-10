package ru.r2cloud.web.api.configuration;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.WebServer;

public class Integrations extends AbstractHttpController {

	private static final String APIKEY_PARAMETER = "apiKey";
	private static final String SYNC_PARAMETER = "syncSpectogram";
	private static final String NEW_LAUNCH_PARAMETER = "newLaunch";
	private static final String SATNOGS_PARAMETER = "satnogs";

	private final Configuration config;

	public Integrations(Configuration config) {
		this.config = config;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		entity.add(APIKEY_PARAMETER, config.getProperty("r2cloud.apiKey"));
		entity.add(SYNC_PARAMETER, config.getBoolean("r2cloud.syncSpectogram"));
		entity.add(NEW_LAUNCH_PARAMETER, config.getBoolean("r2cloud.newLaunches"));
		entity.add(SATNOGS_PARAMETER, config.getBoolean("satnogs.satellites"));
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		String apiKey = WebServer.getString(request, APIKEY_PARAMETER);
		boolean syncSpectogram = WebServer.getBoolean(request, SYNC_PARAMETER);
		boolean newLaunches = WebServer.getBoolean(request, NEW_LAUNCH_PARAMETER);
		boolean satnogs = WebServer.getBoolean(request, SATNOGS_PARAMETER);
		if (apiKey != null && apiKey.length() > 0) {
			config.setProperty("r2cloud.apiKey", apiKey);
		} else {
			config.remove("r2cloud.apiKey");
		}
		config.setProperty("r2cloud.syncSpectogram", String.valueOf(syncSpectogram));
		config.setProperty("r2cloud.newLaunches", String.valueOf(newLaunches));
		config.setProperty("satnogs.satellites", String.valueOf(satnogs));
		config.update();
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/r2cloud";
	}

}
