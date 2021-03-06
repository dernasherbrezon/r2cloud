package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.util.Configuration;

public class SatelliteDao {

	private final Configuration config;
	private final List<Satellite> satellites;
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	public SatelliteDao(Configuration config) {
		this.config = config;
		satellites = new ArrayList<>();
		for (String cur : config.getProperties("satellites.supported")) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			String name = config.getProperty("satellites." + curSatellite.getId() + ".name");
			if (name == null) {
				throw new IllegalStateException("unable to find satellite name for: " + cur);
			}
			curSatellite.setName(name);
			curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
			curSatellite.setSource(FrequencySource.valueOf(config.getProperty("satellites." + curSatellite.getId() + ".source")));
			curSatellite.setEnabled(config.getBoolean("satellites." + curSatellite.getId() + ".enabled"));
			curSatellite.setBandwidth(config.getLong("satellites." + curSatellite.getId() + ".bandwidth"));
			curSatellite.setBaud(config.getInteger("satellites." + curSatellite.getId() + ".baud"));
			index(curSatellite);
		}
		long sdrServerBandwidth = config.getLong("satellites.sdrserver.bandwidth");
		long bandwidthCrop = config.getLong("satellites.sdrserver.bandwidth.crop");
		Collections.sort(satellites, SatelliteComparator.FREQ_COMPARATOR);

		BandFrequency currentBand = null;
		for (Satellite cur : satellites) {
			long lowerSatelliteFrequency = cur.getFrequency() - cur.getBandwidth() / 2;
			long upperSatelliteFrequency = cur.getFrequency() + cur.getBandwidth() / 2;
			// first satellite or upper frequency out of band
			if (currentBand == null || (currentBand.getUpper() - bandwidthCrop) < upperSatelliteFrequency) {
				currentBand = new BandFrequency();
				currentBand.setLower(lowerSatelliteFrequency - bandwidthCrop);
				currentBand.setUpper(currentBand.getLower() + sdrServerBandwidth);
				currentBand.setCenter(currentBand.getLower() + (currentBand.getUpper() - currentBand.getLower()) / 2);
			}
			cur.setFrequencyBand(currentBand);
		}
		Collections.sort(satellites, SatelliteComparator.ID_COMPARATOR);
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

	public List<Satellite> findEnabled() {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : satellites) {
			if (!cur.isEnabled()) {
				continue;
			}
			result.add(cur);
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
