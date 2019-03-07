package ru.r2cloud.web.api.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.ObservationFullComparator;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class ObservationList extends AbstractHttpController {

	private final SatelliteDao dao;
	private final ObservationResultDao resultDao;

	public ObservationList(SatelliteDao dao, ObservationResultDao resultDao) {
		this.dao = dao;
		this.resultDao = resultDao;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		List<Satellite> all = dao.findAll();
		List<ObservationFull> observations = new ArrayList<>();
		for (Satellite cur : all) {
			observations.addAll(resultDao.findAllBySatelliteId(cur.getId()));
		}
		Collections.sort(observations, ObservationFullComparator.INSTANCE);
		JsonArray satellites = new JsonArray();
		for (ObservationFull cur : observations) {
			JsonObject curObservation = new JsonObject();
			curObservation.add("id", cur.getReq().getId());
			curObservation.add("satelliteId", cur.getReq().getSatelliteId());
			Satellite curSatellite = dao.findById(cur.getReq().getSatelliteId());
			if (curSatellite != null) {
				curObservation.add("name", curSatellite.getName());
			} else {
				curObservation.add("name", cur.getReq().getSatelliteId());
			}
			curObservation.add("start", cur.getReq().getStartTimeMillis());
			curObservation.add("hasData", cur.getResult().hasData());
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
