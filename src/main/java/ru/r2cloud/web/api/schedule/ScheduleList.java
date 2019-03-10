package ru.r2cloud.web.api.schedule;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class ScheduleList extends AbstractHttpController {

	private final SatelliteDao satelliteDao;
	private final Scheduler scheduler;

	public ScheduleList(SatelliteDao satelliteDao, Scheduler scheduler) {
		this.satelliteDao = satelliteDao;
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		JsonArray entity = new JsonArray();
		for (Satellite cur : satelliteDao.findAll()) {
			JsonObject curSatellite = new JsonObject();
			curSatellite.add("id", cur.getId());
			curSatellite.add("name", cur.getName());
			curSatellite.add("enabled", cur.isEnabled());
			curSatellite.add("frequency", cur.getFrequency());
			Long nextObservation = scheduler.getNextObservation(cur.getId());
			if (nextObservation != null) {
				curSatellite.add("nextPass", nextObservation);
			}
			entity.add(curSatellite);
		}
		ModelAndView result = new ModelAndView();
		result.setData(entity);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/list";
	}
}
