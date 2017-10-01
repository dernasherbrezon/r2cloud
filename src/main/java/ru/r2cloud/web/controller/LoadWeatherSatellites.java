package ru.r2cloud.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.WeatherObservation;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class LoadWeatherSatellites extends AbstractHttpController {

	private final Configuration config;
	private final SatelliteDao dao;

	public LoadWeatherSatellites(Configuration config, SatelliteDao dao) {
		this.config = config;
		this.dao = dao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("weather");
		result.put("enabled", config.getBoolean("satellites.enabled"));
		List<Satellite> satellites = dao.findSupported();
		Map<Satellite, List<WeatherObservation>> entity = new HashMap<Satellite, List<WeatherObservation>>();
		for (Satellite cur : satellites) {
			entity.put(cur, dao.findWeatherObservations(cur));
		}
		result.put("entity", entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/weather";
	}

}
