package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;

public class TLEDao {

	private static final Logger LOG = LoggerFactory.getLogger(TLEDao.class);

	private final Configuration config;
	private final SatelliteDao satelliteDao;
	private final Path basepath;
	private final CelestrakClient celestrak;

	private final Map<String, Tle> tle = new ConcurrentHashMap<>();

	public TLEDao(Configuration config, SatelliteDao satelliteDao, CelestrakClient celestrak) {
		this.config = config;
		this.satelliteDao = satelliteDao;
		this.basepath = config.getSatellitesBasePath();
		this.celestrak = celestrak;
	}

	public synchronized void start() {
		boolean reload = false;
		long periodMillis = config.getLong("tle.update.periodMillis");
		for (Satellite cur : satelliteDao.findAll()) {
			if (cur.getTle() == null || cur.getTle().getLastUpdateTime() == 0 || System.currentTimeMillis() - cur.getTle().getLastUpdateTime() > periodMillis) {
				reload = true;
				LOG.info("missing or outdated TLE for {}. schedule reloading", cur.getName());
				break;
			}
		}
		// load as much as possible, because celestrak might be unavailable
		// do it on the same thread, as other services might depend on the tle
		// data
		if (reload) {
			reload();
		}
	}

	public Map<String, Tle> findAll() {
		return tle;
	}

	public void reload() {
		Map<String, Tle> newTle = celestrak.getTleForActiveSatellites();
		if (newTle.isEmpty()) {
			return;
		}
		// TLE might come from new launches
		// in this case they are attached to satellite and statically configured
		for (Satellite cur : satelliteDao.findAll()) {
			if (cur.getTle() == null) {
				continue;
			}
			newTle.put(cur.getName(), cur.getTle());
		}
		for (Entry<String, Tle> cur : newTle.entrySet()) {
			Satellite satellite = satelliteDao.findByName(cur.getKey());
			if (satellite == null) {
				continue;
			}
			this.tle.put(satellite.getId(), cur.getValue());
			Path output = basepath.resolve(satellite.getId()).resolve("tle.txt");
			if (!Files.exists(output.getParent())) {
				try {
					Files.createDirectories(output.getParent());
				} catch (IOException e) {
					LOG.error("unable to create directory for satellite: {}", satellite.getName(), e);
					continue;
				}
			}
			// ensure temp and output are on the same filestore
			Path tempOutput = output.getParent().resolve("tle.txt.tmp");
			try (BufferedWriter w = Files.newBufferedWriter(tempOutput)) {
				w.append(cur.getValue().getRaw()[1]);
				w.newLine();
				w.append(cur.getValue().getRaw()[2]);
				w.newLine();
			} catch (IOException e) {
				LOG.error("unable to write tle for: {}", cur.getKey(), e);
				continue;
			}

			try {
				Files.move(tempOutput, output, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				LOG.error("unable to move .tmp to dst", e);
			}
		}
		config.setProperty("satellites.tle.lastupdateAtMillis", System.currentTimeMillis());
		config.update();
	}

	public synchronized void stop() {
		tle.clear();
	}
}
