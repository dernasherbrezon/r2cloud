package ru.r2cloud.web.api.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.ObservationFullComparator;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class ObservationList extends AbstractHttpController {

	private final SatelliteDao dao;
	private final ObservationDao resultDao;

	public ObservationList(SatelliteDao dao, ObservationDao resultDao) {
		this.dao = dao;
		this.resultDao = resultDao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		List<Satellite> all = dao.findAll();
		List<Observation> observations = new ArrayList<>();
		for (Satellite cur : all) {
			observations.addAll(resultDao.findAllBySatelliteId(cur.getId()));
		}
		Collections.sort(observations, ObservationFullComparator.INSTANCE);
		JsonArray satellites = new JsonArray();
		for (Observation cur : observations) {
			JsonObject curObservation = new JsonObject();
			curObservation.add("id", cur.getId());
			curObservation.add("satelliteId", cur.getSatelliteId());
			Satellite curSatellite = dao.findById(cur.getSatelliteId());
			if (curSatellite != null) {
				curObservation.add("name", curSatellite.getName());
			} else {
				curObservation.add("name", cur.getSatelliteId());
			}
			curObservation.add("start", cur.getStartTimeMillis());
			curObservation.add("hasData", cur.hasData());
			satellites.add(curObservation);
		}

		ModelAndView result = new ModelAndView();
		result.setData(satellites.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/observation/list";
	}

}
