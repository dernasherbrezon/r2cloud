package ru.r2cloud.web.controller;

import java.util.logging.Logger;

import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SaveConfiguration extends AbstractHttpController {

	private final static Logger LOG = Logger.getLogger(SaveConfiguration.class.getName());
	
	private final Configuration props;

	public SaveConfiguration(Configuration props) {
		this.props = props;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		Double lat = WebServer.getDouble(session, "lat");
		Double lon = WebServer.getDouble(session, "lon");
		ValidationResult errors = new ValidationResult();
		ConfigurationBean bean = new ConfigurationBean();
		if (lat == null) {
			errors.put("lat", "Cannot be empty");
		} else {
			bean.setLat(String.valueOf(lat));
		}
		if (lon == null) {
			errors.put("lon", "Cannot be empty");
		} else {
			bean.setLon(String.valueOf(lon));
		}
		
		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			ModelAndView result = new ModelAndView("config");
			result.put("errors", errors);
			result.put("entity", bean);
			return result;
		}

		props.put("locaiton.lat", bean.getLat());
		props.put("locaiton.lon", bean.getLon());
		props.update();

		return new Redirect("/admin/config");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config/save";
	}

}
