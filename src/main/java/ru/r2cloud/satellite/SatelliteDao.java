package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SatelliteType;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private final Configuration config;
	private final List<Satellite> satellites;
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	public SatelliteDao(Configuration config) {
		this.config = config;
		satellites = new ArrayList<Satellite>();
		for (String cur : Util.splitComma(config.getProperty("satellites.supported"))) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			curSatellite.setName(config.getProperty("satellites." + curSatellite.getId() + ".name"));
			curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
			curSatellite.setDecoder(config.getProperty("satellites." + curSatellite.getId() + ".decoder"));
			curSatellite.setBandwidth(config.getLong("satellites." + curSatellite.getId() + ".bandwidth"));
			curSatellite.setEnabled(config.getBoolean("satellites." + curSatellite.getId() + ".enabled"));
			if (curSatellite.getDecoder().equals("aausat4")) {
				curSatellite.setType(SatelliteType.AMATEUR);
			} else {
				curSatellite.setType(SatelliteType.WEATHER);
			}
			index(curSatellite);
		}
		Collections.sort(satellites, SatelliteComparator.INSTANCE);
	}

	public Satellite findByName(String name) {
		return satelliteByName.get(name);
	}

	public Satellite findById(String id) {
		return satelliteById.get(id);
	}

	public List<Satellite> findAll() {
		return satellites;
	}

	public List<Satellite> findAll(SatelliteType type) {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : satellites) {
			if (cur.getType().equals(type)) {
				result.add(cur);
			}
		}
		return result;
	}

	private void index(Satellite satellite) {
		satellites.add(satellite);
		satelliteByName.put(satellite.getName(), satellite);
		satelliteById.put(satellite.getId(), satellite);
	}

	public void update(Satellite satelliteToEdit) {
		config.setProperty("satellites." + satelliteToEdit.getId() + ".enabled", satelliteToEdit.isEnabled());
		config.update();
	}

}
