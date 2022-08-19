package ru.r2cloud.tle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class TLEReloader {

	private static final Logger LOG = LoggerFactory.getLogger(TLEReloader.class);
	private final ThreadPoolFactory threadFactory;
	private final SatelliteDao dao;
	private final Configuration config;
	private final Clock clock;
	private final CelestrakClient celestrak;

	private ScheduledExecutorService executor = null;

	public TLEReloader(Configuration config, SatelliteDao dao, ThreadPoolFactory threadFactory, Clock clock, CelestrakClient celestrak) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.clock = clock;
		this.dao = dao;
		this.celestrak = celestrak;
	}

	public synchronized void start() {
		if (executor != null) {
			return;
		}
		boolean reload = false;
		long periodMillis = config.getLong("tle.update.periodMillis");
		for (Satellite cur : dao.findAll()) {
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
		executor = threadFactory.newScheduledThreadPool(1, new NamingThreadFactory("tle-updater"));
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		try {
			long current = clock.millis();
			Date date = sdf.parse(config.getProperty("tle.update.timeUTC"));
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			Calendar executeAt = Calendar.getInstance();
			executeAt.setTimeInMillis(current);
			executeAt.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
			executeAt.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
			executeAt.set(Calendar.SECOND, 0);
			executeAt.set(Calendar.MILLISECOND, 0);
			if (executeAt.getTimeInMillis() < current) {
				executeAt.add(Calendar.MILLISECOND, (int) periodMillis);
			}
			LOG.info("next tle update at: {}", executeAt.getTime());
			executor.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					reload();
				}
			}, executeAt.getTimeInMillis() - current, periodMillis, TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			LOG.info("invalid time. tle will be disabled", e);
		}
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

	public void reload() {
		Map<String, Tle> newTle = celestrak.getTleForActiveSatellites();
		if (newTle.isEmpty()) {
			return;
		}
		for (Satellite satellite : dao.findAll()) {
			Tle tle = newTle.get(satellite.getId());
			if (tle == null) {
				LOG.error("unable to find tle for {}", satellite);
				continue;
			}
			// even if save onto disk fails,
			// make sure Tle is fresh in memory
			satellite.setTle(tle);
			Path output = config.getSatellitesBasePath().resolve(satellite.getId()).resolve("tle.txt");
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
				w.append(tle.getRaw()[1]);
				w.newLine();
				w.append(tle.getRaw()[2]);
				w.newLine();
			} catch (IOException e) {
				LOG.error("unable to write tle for {}", satellite, e);
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
}
