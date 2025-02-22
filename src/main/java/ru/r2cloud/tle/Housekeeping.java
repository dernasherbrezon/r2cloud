package ru.r2cloud.tle;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.cloud.NotModifiedException;
import ru.r2cloud.cloud.SatnogsClient;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class Housekeeping {

	private static final Logger LOG = LoggerFactory.getLogger(Housekeeping.class);

	private final ThreadPoolFactory threadFactory;
	private final SatelliteDao dao;
	private final Configuration config;
	private final CelestrakClient celestrak;
	private final LeoSatDataClient leosatdata;
	private final SatnogsClient satnogs;
	private final TleDao tleDao;
	private final DecoderService decoder;
	private final PriorityService priorityService;
	private final DeviceManager deviceManager;

	private ScheduledExecutorService executor = null;

	public Housekeeping(Configuration config, SatelliteDao dao, ThreadPoolFactory threadFactory, CelestrakClient celestrak, TleDao tleDao, SatnogsClient satnogs, LeoSatDataClient leosatdata, DecoderService decoder, PriorityService priorityService, DeviceManager deviceManager) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.dao = dao;
		this.celestrak = celestrak;
		this.tleDao = tleDao;
		this.satnogs = satnogs;
		this.leosatdata = leosatdata;
		this.decoder = decoder;
		this.priorityService = priorityService;
		this.deviceManager = deviceManager;
	}

	public synchronized void start() {
		if (executor != null) {
			return;
		}
		// must be executed on the same thread for other beans to pick up
		run();
		// can be only null in tests
		if (deviceManager != null) {
			deviceManager.schedule(dao.findAll());
		}
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
		priorityService.reload();
		boolean reloadSatellitesPriority = dao.setPriorities(priorityService.findAll());
		if (reloadSatellites || reloadSatellitesPriority) {
			dao.reindex();
		}
		reloadTle();
		// decoder is null in tests only
		if (decoder != null) {
			decoder.retryObservations();
		}
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
		boolean reloadTle = (System.currentTimeMillis() - tleDao.getLastUpdateTime() > periodMillis);
		if (reloadTle) {
			tleDao.putAll(celestrak.downloadTle());
		} else {
			LOG.info("Skip TLE update. Last update was {}. Next update: {}", new Date(tleDao.getLastUpdateTime()), new Date(tleDao.getLastUpdateTime() + periodMillis));
		}
		// do not store on disk ever growing tle list
		// store only supported satellites
		tleDao.saveTle(dao.setTle(tleDao.findAll()));
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

}
