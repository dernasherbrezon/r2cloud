package ru.r2cloud.util;

import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.model.ObservationFull;
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
			Long nextPass = scheduler.getNextObservation(cur.getId());
			if (nextPass != null) {
				satellite.add("nextPass", nextPass);
			}
			satellite.add("name", cur.getName());
			List<ObservationFull> observations = resultDao.findAllBySatelliteId(cur.getId());
			JsonArray data = new JsonArray();
			for (ObservationFull curObservation : observations) {
				data.add(curObservation.toJson());
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
