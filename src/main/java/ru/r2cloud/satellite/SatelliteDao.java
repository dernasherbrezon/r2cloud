package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SatelliteSource;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterComparator;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private final Configuration config;
	// used for proper overrides during reload
	private final List<Satellite> satnogs = new ArrayList<>();
	private final List<Satellite> staticSatellites = new ArrayList<>();
	private final List<Satellite> leosatdata = new ArrayList<>();
	private final List<Satellite> leosatDataNewLaunches = new ArrayList<>();

	private final List<Satellite> satellites = new ArrayList<>();
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	private long satnogsLastUpdateTime;
	private long leosatdataLastUpdateTime;
	private long leosatdataNewLastUpdateTime;

	public SatelliteDao(Configuration config) {
		this.config = config;
		loadFromDisk();
		reindex();
	}

	public synchronized void saveLeosatdata(List<Satellite> leosatdataSatellites, long currentTime) {
		leosatdata.clear();
		leosatdata.addAll(leosatdataSatellites);
		leosatdataLastUpdateTime = currentTime;
		save(config.getPathFromProperty("satellites.leosatdata.location"), leosatdataSatellites, currentTime);
	}

	public synchronized void saveLeosatdataNew(List<Satellite> loadNewLaunches, long currentTime) {
		leosatDataNewLaunches.clear();
		leosatDataNewLaunches.addAll(loadNewLaunches);
		leosatdataNewLastUpdateTime = currentTime;
		save(config.getPathFromProperty("satellites.leosatdata.new.location"), loadNewLaunches, currentTime);
	}

	public synchronized void saveSatnogs(List<Satellite> loadSatellites, long currentTime) {
		satnogs.clear();
		satnogs.addAll(loadSatellites);
		satnogsLastUpdateTime = currentTime;
		save(config.getPathFromProperty("satellites.satnogs.location"), satnogs, currentTime);
	}

	private void loadFromDisk() {
		satnogsLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty("satellites.satnogs.location"));
		satnogs.addAll(loadFromConfig(config.getPathFromProperty("satellites.satnogs.location"), SatelliteSource.SATNOGS));

		// default from config
		staticSatellites.addAll(loadFromConfig(config.getPathFromProperty("satellites.meta.location"), SatelliteSource.CONFIG));

		leosatdataLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty("satellites.leosatdata.location"));
		leosatdata.addAll(loadFromConfig(config.getPathFromProperty("satellites.leosatdata.location"), SatelliteSource.LEOSATDATA));

		leosatdataNewLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty("satellites.leosatdata.new.location"));
		leosatDataNewLaunches.addAll(loadFromConfig(config.getPathFromProperty("satellites.leosatdata.new.location"), SatelliteSource.LEOSATDATA));
	}

	public synchronized void reindex() {
		satelliteById.clear();
		satellites.clear();
		satelliteByName.clear();

		// 4 different lists of satellites due to:
		// - import from satnogs is automatic and might contain errors
		// - handcrafted configs have better quality
		// - server-side might override both in case of emergency/quick fixes
		// - new launches from satnogs even less reliable and
		// - and everything else should be overriden by leosatdata new launches if any
		if (config.getBoolean("satnogs.satellites")) {
			for (Satellite cur : satnogs) {
				if (cur.getPriority().equals(Priority.NORMAL)) {
					satelliteById.put(cur.getId(), cur);
				}
			}
		}

		// default from config
		for (Satellite cur : staticSatellites) {
			satelliteById.put(cur.getId(), cur);
		}

		if (config.getProperty("r2cloud.apiKey") != null) {
			// new and overrides from server
			for (Satellite cur : leosatdata) {
				satelliteById.put(cur.getId(), cur);
			}
		}

		// optionally new launches
		if (config.getBoolean("r2cloud.newLaunches")) {
			// satnogs new launch ids are: ABXM-4898-9222-6959-5721
			// leosatdata new launch ids are: R2CLOUD123
			// Can't map them using ids. However leosatdata guarantees the same name for new
			// launches as in satnogs
			// use satellite name for deduplication
			Map<String, Satellite> dedupByName = new HashMap<>();
			for (Satellite cur : satnogs) {
				if (cur.getPriority().equals(Priority.HIGH)) {
					dedupByName.put(cur.getName().toLowerCase(), cur);
				}
			}
			if (config.getProperty("r2cloud.apiKey") != null) {
				for (Satellite cur : leosatDataNewLaunches) {
					dedupByName.put(cur.getName().toLowerCase(), cur);
				}
			}
			for (Satellite cur : dedupByName.values()) {
				satelliteById.put(cur.getId(), cur);
			}
		}

		satellites.addAll(satelliteById.values());
		List<Transmitter> allTransmitters = new ArrayList<>();
		for (Satellite curSatellite : satellites) {
			normalize(curSatellite);
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
			satelliteByName.put(curSatellite.getName(), curSatellite);
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
		printStatsByPriorityAndSource();
	}

	private void normalize(Satellite satellite) {
		// user overrides from UI or manually from config
		String enabled = config.getProperty("satellites." + satellite.getId() + ".enabled");
		if (enabled != null) {
			satellite.setEnabled(Boolean.valueOf(enabled));
		}
		for (int i = 0; i < satellite.getTransmitters().size(); i++) {
			Transmitter cur = satellite.getTransmitters().get(i);
			cur.setId(satellite.getId() + "-" + String.valueOf(i));
			cur.setEnabled(satellite.isEnabled());
			cur.setPriority(satellite.getPriority());
			cur.setSatelliteId(satellite.getId());
			cur.setStart(satellite.getStart());
			cur.setEnd(satellite.getEnd());
			cur.setTle(satellite.getTle());
		}
	}

	private static void save(Path location, List<Satellite> satellites, long lastUpdateTime) {
		JsonArray array = new JsonArray();
		for (Satellite cur : satellites) {
			array.add(cur.toJson());
		}
		// it's ok to have temporary name for all type of satellites -
		// save method is synchronized
		Path tempOutput = location.getParent().resolve("satellites.json.tmp");
		try (BufferedWriter w = Files.newBufferedWriter(tempOutput)) {
			array.writeTo(w);
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to save satellites: " + tempOutput.toAbsolutePath(), e);
			return;
		}

		try {
			Files.setLastModifiedTime(tempOutput, FileTime.from(lastUpdateTime, TimeUnit.MILLISECONDS));
		} catch (IOException e) {
			Util.logIOException(LOG, "can't set last modified time", e);
			return;
		}

		try {
			Files.move(tempOutput, location, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			LOG.error("unable to move .tmp to dst", e);
		}
	}

	private static List<Satellite> loadFromConfig(Path metaLocation, SatelliteSource source) {
		List<Satellite> result = new ArrayList<>();
		if (!Files.exists(metaLocation)) {
			return result;
		}
		JsonArray rawSatellites;
		try (BufferedReader r = Files.newBufferedReader(metaLocation)) {
			rawSatellites = Json.parse(r).asArray();
		} catch (Exception e) {
			LOG.error("unable to parse satellites", e);
			return Collections.emptyList();
		}
		for (int i = 0; i < rawSatellites.size(); i++) {
			Satellite cur = Satellite.fromJson(rawSatellites.get(i).asObject());
			cur.setSource(source);
			result.add(cur);
		}
		return result;
	}

	private static long getLastModifiedTimeSafely(Path path) {
		if (!Files.exists(path)) {
			return 0;
		}
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0;
		}
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

	private void printStatsByPriorityAndSource() {
		printStatsBySource(Priority.HIGH);
		printStatsBySource(Priority.NORMAL);
	}

	private void printStatsBySource(Priority priority) {
		int total = 0;
		Map<SatelliteSource, Integer> totalBySource = new HashMap<>();
		for (Satellite cur : satellites) {
			if (!cur.getPriority().equals(priority)) {
				continue;
			}
			total++;
			Integer previous = totalBySource.get(cur.getSource());
			if (previous == null) {
				previous = 0;
			}
			totalBySource.put(cur.getSource(), previous + 1);
		}
		LOG.info("{}: {}", priority, total);
		for (Entry<SatelliteSource, Integer> cur : totalBySource.entrySet()) {
			LOG.info("  {}: {}", cur.getKey(), cur.getValue());
		}
	}

	public long getLeosatdataLastUpdateTime() {
		return leosatdataLastUpdateTime;
	}

	public long getSatnogsLastUpdateTime() {
		return satnogsLastUpdateTime;
	}

	public long getLeosatdataNewLastUpdateTime() {
		return leosatdataNewLastUpdateTime;
	}

}
