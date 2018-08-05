package ru.r2cloud.util;

import java.util.Date;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.satellite.Scheduler;

public class JsonUtil {

	public static JsonObject serialize(Scheduler scheduler, ObservationResultDao resultDao, List<Satellite> supported) {
		JsonObject entity = new JsonObject();
		JsonArray satellites = new JsonArray();
		for (Satellite cur : supported) {
			JsonObject satellite = new JsonObject();
			satellite.add("id", cur.getId());
			Date nextPass = scheduler.getNextObservation(cur.getId());
			if (nextPass != null) {
				satellite.add("nextPass", nextPass.getTime());
			}
			satellite.add("name", cur.getName());
			List<ObservationResult> observations = resultDao.findAllBySatelliteId(cur.getId());
			JsonArray data = new JsonArray();
			for (ObservationResult curObservation : observations) {
				JsonObject curObservationObject = new JsonObject();
				curObservationObject.add("id", curObservation.getId());
				curObservationObject.add("start", curObservation.getStart().getTime());
				curObservationObject.add("aURL", curObservation.getaURL());
				curObservationObject.add("bURL", curObservation.getbURL());
				curObservationObject.add("data", curObservation.getDataURL());
				data.add(curObservationObject);
			}
			satellite.add("data", data);
			satellites.add(satellite);
		}
		entity.add("satellites", satellites);
		return entity;
	}

	private JsonUtil() {
		// do nothing
	}

}
