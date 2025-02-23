package ru.r2cloud.satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SatelliteSource;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SatelliteDao {

	private static final String SATNOGS_LOCATION = "satellites.satnogs.location";
	private static final String LEOSATDATA_NEW_LOCATION = "satellites.leosatdata.new.location";
	private static final String LEOSATDATA_LOCATION = "satellites.leosatdata.location";
	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);
	private static final String CLASSPATH_PREFIX = "classpath:";

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
		save(config.getPathFromProperty(LEOSATDATA_LOCATION), leosatdataSatellites, currentTime);
	}

	public synchronized void saveLeosatdataNew(List<Satellite> loadNewLaunches, long currentTime) {
		leosatDataNewLaunches.clear();
		leosatDataNewLaunches.addAll(loadNewLaunches);
		leosatdataNewLastUpdateTime = currentTime;
		save(config.getPathFromProperty(LEOSATDATA_NEW_LOCATION), loadNewLaunches, currentTime);
	}

	public synchronized void saveSatnogs(List<Satellite> loadSatellites, long currentTime) {
		satnogs.clear();
		satnogs.addAll(loadSatellites);
		satnogsLastUpdateTime = currentTime;
		save(config.getPathFromProperty(SATNOGS_LOCATION), satnogs, currentTime);
	}

	public synchronized boolean setPriorities(Map<String, Integer> priorities) {
		boolean result = false;
		for (Satellite cur : findAll()) {
			Integer priority = priorities.get(cur.getId());
			if (priority == null) {
				continue;
			}
			if (cur.getPriorityIndex() != priority) {
				result = true;
			}
			cur.setPriorityIndex(priority);
		}
		return result;
	}

	public synchronized Map<String, Tle> setTle(Map<String, Tle> tle) {
		Map<String, Tle> updated = new HashMap<>();
		for (Satellite cur : findAll()) {
			Tle oldTle = cur.getTle();
			Tle newTle = tle.get(cur.getId());
			if (oldTle == null && newTle == null) {
				continue;
			}
			if (oldTle == null && newTle != null) {
				cur.setTle(newTle);
			}
			if (oldTle != null && newTle == null) {
				cur.setTle(oldTle);
			}
			if (oldTle != null && newTle != null) {
				// always update to new one
				// even if it is the same
				cur.setTle(newTle);
			}
			updated.put(cur.getId(), cur.getTle());
		}
		return updated;
	}

	private void loadFromDisk() {
		satnogsLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty(SATNOGS_LOCATION));
		satnogs.addAll(loadFromConfig(config.getPathFromProperty(SATNOGS_LOCATION), SatelliteSource.SATNOGS, satnogsLastUpdateTime));

		// default from config
		String metaLocation = config.getProperty("satellites.meta.location");
		if (metaLocation.startsWith("classpath:")) {
			staticSatellites.addAll(loadFromClasspathConfig(metaLocation.substring(CLASSPATH_PREFIX.length()), SatelliteSource.CONFIG));
		} else {
			long staticLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty("satellites.meta.location"));
			staticSatellites.addAll(loadFromConfig(config.getPathFromProperty("satellites.meta.location"), SatelliteSource.CONFIG, staticLastUpdateTime));
		}

		leosatdataLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty(LEOSATDATA_LOCATION));
		leosatdata.addAll(loadFromConfig(config.getPathFromProperty(LEOSATDATA_LOCATION), SatelliteSource.LEOSATDATA, leosatdataLastUpdateTime));

		leosatdataNewLastUpdateTime = getLastModifiedTimeSafely(config.getPathFromProperty(LEOSATDATA_NEW_LOCATION));
		leosatDataNewLaunches.addAll(loadFromConfig(config.getPathFromProperty(LEOSATDATA_NEW_LOCATION), SatelliteSource.LEOSATDATA, leosatdataNewLastUpdateTime));
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
			indexSatellite(cur);
		}

		if (config.getProperty("r2cloud.apiKey") != null) {
			// new and overrides from server
			for (Satellite cur : leosatdata) {
				indexSatellite(cur);
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
				indexSatellite(cur);
			}
		}

		satellites.addAll(satelliteById.values());
		for (Satellite curSatellite : satellites) {
			normalize(curSatellite);
			satelliteByName.put(curSatellite.getName(), curSatellite);
		}
		Collections.sort(satellites, SatelliteComparator.ID_COMPARATOR);
		printStatsByPriorityAndSource();
	}

	private void indexSatellite(Satellite sat) {
		Satellite old = satelliteById.get(sat.getId());
		if (old == null || old.getLastUpdateTime() <= sat.getLastUpdateTime()) {
			satelliteById.put(sat.getId(), sat);
			return;
		}
	}

	private void normalize(Satellite satellite) {
		// user overrides from UI or manually from config
		String enabled = config.getProperty("satellites." + satellite.getId() + ".enabled");
		if (enabled != null) {
			satellite.setEnabled(Boolean.valueOf(enabled));
		}
		for (int i = 0; i < satellite.getTransmitters().size(); i++) {
			Transmitter cur = satellite.getTransmitters().get(i);
			cur.setId(satellite.getId() + "-" + i);
			cur.setEnabled(satellite.isEnabled());
			cur.setPriority(satellite.getPriority());
			cur.setPriorityIndex(satellite.getPriorityIndex());
			cur.setSatelliteId(satellite.getId());
			cur.setStart(satellite.getStart());
			cur.setEnd(satellite.getEnd());
			cur.setTle(satellite.getTle());
			cur.setFrequencyBand(cur.getFrequency());
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

	public static List<Satellite> loadFromClasspathConfig(String metaLocation, SatelliteSource source) {
		List<Satellite> result = new ArrayList<>();
		InputStream is = SatelliteDao.class.getClassLoader().getResourceAsStream(metaLocation);
		if (is == null) {
			return result;
		}
		JsonArray rawSatellites;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			rawSatellites = Json.parse(r).asArray();
		} catch (Exception e) {
			LOG.error("unable to parse satellites", e);
			return result;
		}
		for (int i = 0; i < rawSatellites.size(); i++) {
			Satellite cur = Satellite.fromJson(rawSatellites.get(i).asObject());
			if (cur == null) {
				continue;
			}
			cur.setSource(source);
			result.add(cur);
		}
		return result;
	}

	private static List<Satellite> loadFromConfig(Path metaLocation, SatelliteSource source, long lastUpdateTime) {
		List<Satellite> result = new ArrayList<>();
		if (!Files.exists(metaLocation)) {
			return result;
		}
		JsonArray rawSatellites;
		try (BufferedReader r = Files.newBufferedReader(metaLocation)) {
			rawSatellites = Json.parse(r).asArray();
		} catch (Exception e) {
			LOG.error("unable to parse satellites", e);
			return result;
		}
		for (int i = 0; i < rawSatellites.size(); i++) {
			Satellite cur = Satellite.fromJson(rawSatellites.get(i).asObject());
			if (cur == null) {
				continue;
			}
			cur.setSource(source);
			if (cur.getLastUpdateTime() == 0) {
				cur.setLastUpdateTime(lastUpdateTime);
			}
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
