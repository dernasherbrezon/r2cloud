package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2ServerClient;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.util.Configuration;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private final Configuration config;
	private final R2ServerClient r2server;
	private final List<Satellite> satellites = new ArrayList<>();
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	public SatelliteDao(Configuration config, R2ServerClient r2server) {
		this.config = config;
		this.r2server = r2server;
		reload();
	}

	public synchronized void reload() {
		satellites.clear();
		satelliteByName.clear();
		satelliteById.clear();

		satellites.addAll(loadFromConfig(config));
		if (config.getBoolean("r2cloud.newLaunches")) {
			satellites.addAll(r2server.loadNewLaunches());
		}
		for (Satellite curSatellite : satellites) {
			switch (curSatellite.getSource()) {
			case APT:
				curSatellite.setInputSampleRate(60_000);
				curSatellite.setOutputSampleRate(11_025);
				break;
			case LRPT:
				curSatellite.setInputSampleRate(288_000);
				curSatellite.setOutputSampleRate(144_000);
				break;
			case TELEMETRY:
				// sdr-server supports very narrow bandwidths
				int outputSampleRate = 48_000;
				// some rates better to sample at 50k
				if (checkBaudRate(curSatellite, 50_000)) {
					outputSampleRate = 50_000;
				}
				if (config.getSdrType().equals(SdrType.SDRSERVER)) {
					curSatellite.setInputSampleRate(outputSampleRate);
					curSatellite.setOutputSampleRate(outputSampleRate);
				} else if (curSatellite.getModulation() != null && curSatellite.getModulation().equals(Modulation.LORA)) {
					// not applicable
					curSatellite.setInputSampleRate(0);
					curSatellite.setOutputSampleRate(0);
				} else {
					// 48k * 5 = 240k - minimum rate rtl-sdr supports
					curSatellite.setInputSampleRate(outputSampleRate * 5);
					curSatellite.setOutputSampleRate(outputSampleRate);
				}
				break;
			default:
				throw new IllegalArgumentException("unsupported source: " + curSatellite.getSource());
			}
			index(curSatellite);
		}
		long sdrServerBandwidth = config.getLong("satellites.sdrserver.bandwidth");
		long bandwidthCrop = config.getLong("satellites.sdrserver.bandwidth.crop");
		Collections.sort(satellites, SatelliteComparator.FREQ_BANDWIDTH_COMPARATOR);

		BandFrequency currentBand = null;
		for (Satellite cur : satellites) {
			long lowerSatelliteFrequency = cur.getFrequency() - cur.getInputSampleRate() / 2;
			long upperSatelliteFrequency = cur.getFrequency() + cur.getInputSampleRate() / 2;
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

	@SuppressWarnings("unchecked")
	private static List<Satellite> loadFromConfig(Configuration config) {
		List<Satellite> result = new ArrayList<>();
		for (String cur : config.getProperties("satellites.supported")) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			String name = config.getProperty("satellites." + curSatellite.getId() + ".name");
			if (name == null) {
				throw new IllegalStateException("unable to find satellite name for: " + cur);
			}
			curSatellite.setName(name);
			curSatellite.setPriority(Priority.NORMAL);
			curSatellite.setFrequency(config.getLong("satellites." + curSatellite.getId() + ".freq"));
			curSatellite.setSource(FrequencySource.valueOf(config.getProperty("satellites." + curSatellite.getId() + ".source")));
			curSatellite.setEnabled(config.getBoolean("satellites." + curSatellite.getId() + ".enabled"));
			Long bandwidth = config.getLong("satellites." + curSatellite.getId() + ".bandwidth");
			if (bandwidth != null) {
				curSatellite.setBandwidth(bandwidth);
			}
			curSatellite.setBaudRates(config.getIntegerList("satellites." + curSatellite.getId() + ".baud"));
			String modulationStr = config.getProperty("satellites." + curSatellite.getId() + ".modulation");
			if (modulationStr != null) {
				curSatellite.setModulation(Modulation.valueOf(modulationStr));
			}
			if (curSatellite.getBaudRates().isEmpty() && (curSatellite.getModulation() == null || !curSatellite.getModulation().equals(Modulation.LORA)) && curSatellite.getSource().equals(FrequencySource.TELEMETRY)) {
				LOG.error("baud rates are missing for satellite: {}", curSatellite.getId());
			}

			String framingStr = config.getProperty("satellites." + curSatellite.getId() + ".framing");
			if (framingStr != null) {
				curSatellite.setFraming(Framing.valueOf(framingStr));
			}
			String beaconClassStr = config.getProperty("satellites." + curSatellite.getId() + ".beacon");
			if (beaconClassStr != null) {
				try {
					curSatellite.setBeaconClass((Class<? extends Beacon>) Class.forName(beaconClassStr));
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(e);
				}
			}
			String beaconSizeStr = config.getProperty("satellites." + curSatellite.getId() + ".beaconSize");
			if (beaconSizeStr != null) {
				curSatellite.setBeaconSizeBytes(Integer.valueOf(beaconSizeStr));
			}

			Long loraBandwidth = config.getLong("satellites." + curSatellite.getId() + ".loraBw");
			if (loraBandwidth != null) {
				curSatellite.setLoraBandwidth(loraBandwidth);
			}
			Integer loraSpreadFactor = config.getInteger("satellites." + curSatellite.getId() + ".loraSf");
			if (loraSpreadFactor != null) {
				curSatellite.setLoraSpreadFactor(loraSpreadFactor);
			}
			Integer loraCodingRate = config.getInteger("satellites." + curSatellite.getId() + ".loraCr");
			if (loraCodingRate != null) {
				curSatellite.setLoraCodingRate(loraCodingRate);
			}
			Integer loraSyncword = config.getInteger("satellites." + curSatellite.getId() + ".loraSyncword");
			if (loraSyncword != null) {
				curSatellite.setLoraSyncword(loraSyncword);
			}
			Integer loraPreambleLength = config.getInteger("satellites." + curSatellite.getId() + ".loraPreambleLength");
			if (loraPreambleLength != null) {
				curSatellite.setLoraPreambleLength(loraPreambleLength);
			}
			Integer loraLdro = config.getInteger("satellites." + curSatellite.getId() + ".loraLdro");
			if (loraLdro != null) {
				curSatellite.setLoraLdro(loraLdro);
			}

			result.add(curSatellite);
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

	public synchronized List<Satellite> findByFilter(SatelliteFilter filter) {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : satellites) {
			if (filter.accept(cur)) {
				result.add(cur);
			}
		}
		return result;
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

	private static boolean checkBaudRate(Satellite satellite, int outputSampleRate) {
		if (satellite.getBaudRates() == null || satellite.getBaudRates().isEmpty()) {
			return false;
		}
		return (outputSampleRate % satellite.getBaudRates().get(0) == 0);
	}

}
