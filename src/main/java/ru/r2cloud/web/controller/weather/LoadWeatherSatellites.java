package ru.r2cloud.web.controller.weather;

import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.WeatherSatellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class LoadWeatherSatellites extends AbstractHttpController {

	private final Configuration config;
	private final SatelliteDao dao;
	private final Scheduler scheduler;

	public LoadWeatherSatellites(Configuration config, SatelliteDao dao, Scheduler scheduler) {
		this.config = config;
		this.dao = dao;
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String errors = WebServer.getParameter(session, "errors");
		ModelAndView result = new ModelAndView("weather");
		result.put("enabled", config.getBoolean("satellites.enabled"));
		List<Satellite> satellites = dao.findSupported();
		List<WeatherSatellite> entity = new ArrayList<WeatherSatellite>(satellites.size());
		for (Satellite cur : satellites) {
			WeatherSatellite curData = new WeatherSatellite();
			curData.setData(dao.findWeatherObservations(cur));
			curData.setSatellite(cur);
			curData.setNext(scheduler.getNextObservation(cur.getId()));
			entity.add(curData);
		}
		result.put("entity", entity);
		if (errors != null) {
			result.put("errors", ValidationResult.valueOf("agreeToC", errors));
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/weather";
	}

}
