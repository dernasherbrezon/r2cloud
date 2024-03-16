package ru.r2cloud.tle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.cloud.NotModifiedException;
import ru.r2cloud.cloud.SatnogsClient;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class Housekeeping {

	private final ThreadPoolFactory threadFactory;
	private final SatelliteDao dao;
	private final Configuration config;
	private final CelestrakClient celestrak;
	private final LeoSatDataClient leosatdata;
	private final SatnogsClient satnogs;
	private final TleDao tleDao;
	private final DecoderService decoder;
	private final PriorityService priorityService;

	private ScheduledExecutorService executor = null;

	public Housekeeping(Configuration config, SatelliteDao dao, ThreadPoolFactory threadFactory, CelestrakClient celestrak, TleDao tleDao, SatnogsClient satnogs, LeoSatDataClient leosatdata, DecoderService decoder, PriorityService priorityService) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.dao = dao;
		this.celestrak = celestrak;
		this.tleDao = tleDao;
		this.satnogs = satnogs;
		this.leosatdata = leosatdata;
		this.decoder = decoder;
		this.priorityService = priorityService;
	}

	public synchronized void start() {
		if (executor != null) {
			return;
		}
		// must be executed on the same thread for other beans to pick up
		run();
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
		boolean reloadSatellites = reloadSatellites();
		boolean reloadSatellitesPriority = reloadPriority();
		if (reloadSatellites || reloadSatellitesPriority) {
			dao.reindex();
		}
		reloadTle();
		// decoder is null in tests only
		if (decoder != null) {
			decoder.retryObservations();
		}
	}

	private boolean reloadPriority() {
		priorityService.reload();
		boolean result = false;
		for (Satellite cur : dao.findAll()) {
			Integer priority = priorityService.find(cur.getId());
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

	private boolean reloadSatellites() {
		long currentTime = System.currentTimeMillis();

		boolean atLeastOneReloaded = false;

		if (config.getBoolean("satnogs.satellites")) {
			long periodMillis = config.getLong("housekeeping.satnogs.periodMillis");
			boolean reload = currentTime - dao.getSatnogsLastUpdateTime() > periodMillis;
			if (reload) {
				atLeastOneReloaded = true;
				dao.saveSatnogs(satnogs.loadSatellites(), currentTime);
			}
		}

		if (config.getProperty("r2cloud.apiKey") != null) {
			long periodMillis = config.getLong("housekeeping.leosatdata.periodMillis");
			boolean reload = currentTime - dao.getLeosatdataLastUpdateTime() > periodMillis;
			if (reload) {
				try {
					dao.saveLeosatdata(leosatdata.loadSatellites(dao.getLeosatdataLastUpdateTime()), currentTime);
					atLeastOneReloaded = true;
				} catch (NotModifiedException e) {
					// do not update the data on-disk
				}
			}

			if (config.getBoolean("r2cloud.newLaunches")) {
				periodMillis = config.getLong("housekeeping.leosatdata.new.periodMillis");
				reload = currentTime - dao.getLeosatdataNewLastUpdateTime() > periodMillis;
				if (reload) {
					try {
						dao.saveLeosatdataNew(leosatdata.loadNewLaunches(dao.getLeosatdataNewLastUpdateTime()), currentTime);
						atLeastOneReloaded = true;
					} catch (NotModifiedException e) {
						// do not update the data on-disk
					}
				}
			}

		}

		return atLeastOneReloaded;
	}

	private void reloadTle() {
		long periodMillis = config.getLong("housekeeping.tle.periodMillis");
		// do not store on disk ever growing tle list
		// store only supported satellites
		Map<String, Tle> updated = new HashMap<>();
		boolean reloadTle = (System.currentTimeMillis() - tleDao.getLastUpdateTime() > periodMillis);
		if (reloadTle) {
			tleDao.putAll(celestrak.downloadTle());
		}
		for (Satellite cur : dao.findAll()) {
			Tle curTle;
			if (cur.getTle() != null) {
				curTle = cur.getTle();
			} else {
				curTle = tleDao.find(cur.getId(), cur.getName());
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
