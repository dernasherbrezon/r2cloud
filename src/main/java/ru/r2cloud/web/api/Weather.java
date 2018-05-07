package ru.r2cloud.web.api;

import java.util.Date;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Weather extends AbstractHttpController {

	private final Configuration config;
	private final SatelliteDao dao;
	private final ObservationResultDao resultDao;
	private final Scheduler scheduler;

	public Weather(Configuration config, SatelliteDao dao, Scheduler scheduler, ObservationResultDao resultDao) {
		this.config = config;
		this.dao = dao;
		this.resultDao = resultDao;
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		boolean isEnabled = config.getBoolean("satellites.enabled");
		entity.add("enabled", isEnabled);
		if (isEnabled) {
			JsonArray satellites = new JsonArray();
			List<Satellite> supported = dao.findSupported();
			for (Satellite cur : supported) {
				JsonObject satellite = new JsonObject();
				satellite.add("id", cur.getId());
				Date nextPass = scheduler.getNextObservation(cur.getId());
				if (nextPass != null) {
					satellite.add("nextPass", nextPass.getTime());
				}
				satellite.add("name", cur.getName());
				List<ObservationResult> observations = resultDao.findWeatherObservations(cur.getId());
				JsonArray data = new JsonArray();
				for (ObservationResult curObservation : observations) {
					JsonObject curObservationObject = new JsonObject();
					curObservationObject.add("id", curObservation.getId());
					curObservationObject.add("start", curObservation.getStart().getTime());
					curObservationObject.add("aURL", curObservation.getaURL());
					curObservationObject.add("bURL", curObservation.getbURL());
					data.add(curObservationObject);
				}
				satellite.add("data", data);
				satellites.add(satellite);
			}
			entity.add("satellites", satellites);
		}
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather";
	}

}
