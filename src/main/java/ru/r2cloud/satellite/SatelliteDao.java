package ru.r2cloud.satellite;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterComparator;
import ru.r2cloud.util.Configuration;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private final Configuration config;
	private final LeoSatDataClient client;
	private final List<Satellite> satellites = new ArrayList<>();
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	public SatelliteDao(Configuration config, LeoSatDataClient client) {
		this.config = config;
		this.client = client;
		reload();
	}

	public synchronized void reload() {
		satellites.clear();
		satelliteByName.clear();
		satelliteById.clear();

		satellites.addAll(loadFromConfig(config.getProperty("satellites.meta.location"), config));
		if (config.getBoolean("r2cloud.newLaunches")) {
			satellites.addAll(client.loadNewLaunches());
		}
		List<Transmitter> allTransmitters = new ArrayList<>();
		for (Satellite curSatellite : satellites) {
			allTransmitters.addAll(curSatellite.getTransmitters());
			for (Transmitter curTransmitter : curSatellite.getTransmitters()) {
				switch (curTransmitter.getFraming()) {
				case APT:
					curTransmitter.setInputSampleRate(60_000);
					curTransmitter.setOutputSampleRate(11_025);
					break;
				case LRPT:
					curTransmitter.setInputSampleRate(288_000);
					curTransmitter.setOutputSampleRate(144_000);
					break;
				default:
					// sdr-server supports very narrow bandwidths
					int outputSampleRate = 48_000;
					if (config.getSdrType().equals(SdrType.SDRSERVER)) {
						curTransmitter.setInputSampleRate(outputSampleRate);
						curTransmitter.setOutputSampleRate(outputSampleRate);
					} else if (curTransmitter.getModulation() != null && curTransmitter.getModulation().equals(Modulation.LORA)) {
						// not applicable
						curTransmitter.setInputSampleRate(0);
						curTransmitter.setOutputSampleRate(0);
					} else {
						// some rates better to sample at 50k
						if (curTransmitter.getBaudRates() != null && curTransmitter.getBaudRates().size() > 0 && 50_000 % curTransmitter.getBaudRates().get(0) == 0) {
							outputSampleRate = 50_000;
						}
						// 48k * 5 = 240k - minimum rate rtl-sdr supports
						curTransmitter.setInputSampleRate(outputSampleRate * 5);
						curTransmitter.setOutputSampleRate(outputSampleRate);
					}
					break;
				}
			}
			index(curSatellite);
		}
		long sdrServerBandwidth = config.getLong("satellites.sdrserver.bandwidth");
		long bandwidthCrop = config.getLong("satellites.sdrserver.bandwidth.crop");
		Collections.sort(allTransmitters, TransmitterComparator.INSTANCE);

		// bands can be calculated only when all supported transmitters known
		BandFrequency currentBand = null;
		for (Transmitter cur : allTransmitters) {
			long lowerSatelliteFrequency = cur.getFrequency() - cur.getInputSampleRate() / 2;
			long upperSatelliteFrequency = cur.getFrequency() + cur.getInputSampleRate() / 2;
			// first transmitter or upper frequency out of band
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

	private static List<Satellite> loadFromConfig(String metaLocation, Configuration config) {
		List<Satellite> result = new ArrayList<>();
		JsonArray rawSatellites;
		try (Reader r = new InputStreamReader(SatelliteDao.class.getClassLoader().getResourceAsStream(metaLocation))) {
			rawSatellites = Json.parse(r).asArray();
		} catch (Exception e) {
			LOG.error("unable to parse satellites", e);
			return Collections.emptyList();
		}
		for (int i = 0; i < rawSatellites.size(); i++) {
			Satellite satellite = Satellite.fromJson(rawSatellites.get(i).asObject());
			// user-specific
			String enabled = config.getProperty("satellites." + satellite.getId() + ".enabled");
			if (enabled != null) {
				satellite.setEnabled(Boolean.valueOf(enabled));
			}
			result.add(satellite);
		}
		return result;
	}

	public synchronized Satellite findByName(String name) {
		return satelliteByName.get(name);
	}

	public synchronized Satellite findById(String id) {
		return satelliteById.get(id);
	}

	public synchronized List<Satellite> findAll() {
		return satellites;
	}

	private void index(Satellite satellite) {
		satelliteByName.put(satellite.getName(), satellite);
		satelliteById.put(satellite.getId(), satellite);
	}

	public void update(Satellite satelliteToEdit) {
		config.setProperty("satellites." + satelliteToEdit.getId() + ".enabled", satelliteToEdit.isEnabled());
		config.update();
	}

	public synchronized List<Satellite> findEnabled() {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : satellites) {
			if (cur.isEnabled()) {
				result.add(cur);
			}
		}
		return result;
	}

}
