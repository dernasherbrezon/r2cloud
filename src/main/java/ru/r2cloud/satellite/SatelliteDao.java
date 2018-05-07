package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private final List<Satellite> satellites;
	private final Map<String, Satellite> satelliteByName = new HashMap<String, Satellite>();

	public SatelliteDao(Configuration config) {
		satellites = new ArrayList<Satellite>();
		for (String cur : Util.splitComma(config.getProperty("satellites.supported"))) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			curSatellite.setName(config.getProperty("satellites." + curSatellite.getId() + ".name"));
			curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
			curSatellite.setDecoder(config.getProperty("satellites." + curSatellite.getId() + ".decoder"));
			index(curSatellite);
		}
	}

	public Satellite findByName(String name) {
		return satelliteByName.get(name);
	}

	public List<Satellite> findAll() {
		return satellites;
	}

	private void index(Satellite satellite) {
		satellites.add(satellite);
		satelliteByName.put(satellite.getName(), satellite);
	}

}
