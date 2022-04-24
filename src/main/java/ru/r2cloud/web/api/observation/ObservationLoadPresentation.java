package ru.r2cloud.web.api.observation;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class ObservationLoadPresentation extends AbstractHttpController {

	private final ObservationLoad impl;
	private final Configuration config;

	public ObservationLoadPresentation(Configuration config, ObservationDao resultDao, SignedURL signed, SatelliteDao satelliteDao) {
		this.config = config;
		this.impl = new ObservationLoad(resultDao, signed, satelliteDao);
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		boolean enabled = config.getBoolean("presentationMode");
		ModelAndView result = new ModelAndView();
		if (!enabled) {
			result.setStatus(Response.Status.UNAUTHORIZED);
			result.setData("{}");
			return result;
		}
		return impl.doGet(session);
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/observation/load";
	}

}
