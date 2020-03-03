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

	private final Map<String, Tle> tle = new ConcurrentHashMap<String, Tle>();

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
			Path tleFile = basepath.resolve(cur.getId()).resolve("tle.txt");
			if (!Files.exists(tleFile)) {
				LOG.info("missing tle for {}. schedule reloading", cur.getName());
				reload = true;
				continue;
			}
			try {
				long time = Files.getLastModifiedTime(tleFile).toMillis();
				if (System.currentTimeMillis() - time > periodMillis) {
					LOG.info("tle file: {} stale. Last updated at: {}. schedule reloading", tleFile.toAbsolutePath(), new Date(time));
					reload = true;
					// schedule reload, but read it anyway in case celestrak is not
					// available. better to get stale results, rather than none
				}
			} catch (IOException e1) {
				LOG.error("unable to get last modified time. schedule reloading", e1);
				reload = true;
			}
			try (BufferedReader r = Files.newBufferedReader(tleFile)) {
				String line1 = r.readLine();
				if (line1 == null) {
					continue;
				}
				String line2 = r.readLine();
				if (line2 == null) {
					continue;
				}
				this.tle.put(cur.getId(), new Tle(new String[] { cur.getName(), line1, line2 }));
			} catch (IOException e) {
				LOG.error("unable to load TLE for " + cur.getId(), e);
				reload = true;
				continue;
			}
		}
		// load as much as possible, because celestrak might be unavailable
		// do it on the same thread, as other services might depend on the tle
		// data
		if (reload) {
			reload();
		}
	}

	public Tle findById(String id) {
		return tle.get(id);
	}

	public Map<String, Tle> findAll() {
		return tle;
	}

	void reload() {
		Map<String, Tle> newTle = celestrak.getTleForActiveSatellites();
		if (newTle.isEmpty()) {
			return;
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
					LOG.error("unable to create directory for satellite: " + satellite.getName(), e);
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
				LOG.error("unable to write tle for: " + cur.getKey(), e);
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
