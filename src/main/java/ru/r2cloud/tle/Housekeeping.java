package ru.r2cloud.tle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class Housekeeping {

	private final ThreadPoolFactory threadFactory;
	private final SatelliteDao dao;
	private final Configuration config;
	private final CelestrakClient celestrak;
	private final TleDao tleDao;

	private ScheduledExecutorService executor = null;

	public Housekeeping(Configuration config, SatelliteDao dao, ThreadPoolFactory threadFactory, CelestrakClient celestrak, TleDao tleDao) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.dao = dao;
		this.celestrak = celestrak;
		this.tleDao = tleDao;
	}

	public synchronized void start() {
		if (executor != null) {
			return;
		}
		reloadTle();
		long periodMillis = config.getLong("housekeeping.periodMillis");
		executor = threadFactory.newScheduledThreadPool(1, new NamingThreadFactory("housekeeping"));
		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				Housekeeping.this.run();
			}
		}, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
	}

	public void run() {
		reloadTle();
	}

	private void reloadTle() {
		long periodMillis = config.getLong("tle.update.periodMillis");
		Map<String, Tle> tle = tleDao.loadTle();
		// do not store on disk ever growing tle list
		// store only supproted satellites
		Map<String, Tle> updated = new HashMap<>();
		boolean reloadTle = System.currentTimeMillis() - tleDao.getLastUpdateTime() > periodMillis;
		if (reloadTle) {
			tle.putAll(celestrak.downloadTle());
		}
		for (Satellite cur : dao.findAll()) {
			Tle curTle;
			if (cur.getTle() != null) {
				curTle = cur.getTle();
			} else {
				curTle = tle.get(cur.getId());
			}
			cur.setTle(curTle);
			if (curTle != null) {
				updated.put(cur.getId(), cur.getTle());
			}
		}
		if (reloadTle) {
			tleDao.saveTle(updated);
		}
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

}
