package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class TLEDao {

	private static final Logger LOG = LoggerFactory.getLogger(TLEDao.class);

	private final Configuration config;
	private final SatelliteDao satelliteDao;
	private final File basepath;
	private final CelestrakClient celestrak;

	private final Map<String, Tle> tle = new ConcurrentHashMap<String, Tle>();

	public TLEDao(Configuration config, SatelliteDao satelliteDao, CelestrakClient celestrak) {
		this.config = config;
		this.satelliteDao = satelliteDao;
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
		this.celestrak = celestrak;
	}

	public synchronized void start() {
		boolean reload = false;
		for (Satellite cur : satelliteDao.findAll()) {
			File tleFile = new File(basepath, cur.getId() + File.separator + "tle.txt");
			if (!tleFile.exists()) {
				LOG.info("missing tle for {}. schedule reloading", cur.getName());
				reload = true;
				continue;
			}
			if (System.currentTimeMillis() - tleFile.lastModified() > TimeUnit.DAYS.toMillis(7)) {
				LOG.info("tle file: {} stale. Last updated at: {}. schedule reloading", tleFile.getAbsolutePath(), new Date(tleFile.lastModified()));
				reload = true;
				// shcedule reload, but read it anyway in case celestrak is not
				// available. better to get stale results, rather than none
			}
			try (BufferedReader r = new BufferedReader(new FileReader(tleFile))) {
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
		Map<String, Tle> tle = celestrak.getTleForActiveSatellites();
		if (tle.isEmpty()) {
			return;
		}
		for (Entry<String, Tle> cur : tle.entrySet()) {
			Satellite satellite = satelliteDao.findByName(cur.getKey());
			if (satellite == null) {
				continue;
			}
			this.tle.put(satellite.getId(), cur.getValue());
			File output = new File(basepath, satellite.getId() + File.separator + "tle.txt");
			if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
				LOG.error("unable to create directory for satellite: {}", satellite.getName());
				continue;
			}
			try (BufferedWriter w = new BufferedWriter(new FileWriter(output))) {
				w.append(cur.getValue().getRaw()[1]);
				w.newLine();
				w.append(cur.getValue().getRaw()[2]);
				w.newLine();
			} catch (IOException e) {
				LOG.error("unable to write tle for: " + cur.getKey(), e);
			}
		}
		config.setProperty("satellites.tle.lastupdateAtMillis", System.currentTimeMillis());
		config.update();
	}

	public synchronized void stop() {
		tle.clear();
	}
}
