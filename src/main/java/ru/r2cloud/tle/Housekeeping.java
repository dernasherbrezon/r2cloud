package ru.r2cloud.tle;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.cloud.NotModifiedException;
import ru.r2cloud.cloud.SatnogsClient;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.util.Clock;
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
	private final Clock clock;

	private ScheduledExecutorService executor = null;
	private long lastPredictMillis;

	public Housekeeping(Configuration config, SatelliteDao dao, ThreadPoolFactory threadFactory, CelestrakClient celestrak, TleDao tleDao, SatnogsClient satnogs, LeoSatDataClient leosatdata, DecoderService decoder, PriorityService priorityService, DeviceManager deviceManager, Clock clock) {
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
		this.clock = clock;
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
		long currentTime = clock.millis();

		priorityService.reload();
		boolean reloadSatellitesPriority = dao.setPriorities(priorityService.findAll());

		Map<String, Satellite> previous = index(dao.findAll());
		boolean reloadSatellites = reloadSatellites(currentTime);
		if (reloadSatellites || reloadSatellitesPriority) {
			dao.reindex();
			dao.setTle(tleDao.findAll());
		}

		Map<String, Satellite> current = index(dao.findAll());

		reloadTle(currentTime);
		long predictPeriod = config.getLong("housekeeping.predictMillis");
		// can be only null in tests
		if (deviceManager != null) {
			if (currentTime - lastPredictMillis >= predictPeriod) {
				if (lastPredictMillis == 0) {
					deviceManager.schedule(dao.findAll());
				} else {
					deviceManager.reschedule();
				}
				lastPredictMillis = currentTime;
				LOG.info("observations re-scheduled. next update: {}", new Date(currentTime + predictPeriod));
			} else {
				// some satellites can move from new launches to normal. thus change id.
				// some satellite might be removed
				for (Satellite cur : current.values()) {
					if (previous.containsKey(cur.getId())) {
						continue;
					}
					deviceManager.schedule(cur);
				}
				boolean reschedule = false;
				for (Satellite cur : previous.values()) {
					if (current.containsKey(cur.getId())) {
						continue;
					}
					// disabled satellites won't be scheduled
					cur.setEnabled(false);
					reschedule = true;
					LOG.info("disabling satellite observations: {}", cur.getId());
				}
				if (reschedule) {
					deviceManager.reschedule();
				}
			}
		}
		// decoder is null in tests only
		if (decoder != null) {
			decoder.retryObservations();
		}
	}

	private static Map<String, Satellite> index(List<Satellite> sat) {
		Map<String, Satellite> result = new HashMap<>();
		for (Satellite cur : sat) {
			result.put(cur.getId(), cur);
		}
		return result;
	}

	private boolean reloadSatellites(long currentTime) {

		boolean atLeastOneReloaded = false;

		if (config.getBoolean("satnogs.satellites")) {
			long periodMillis = config.getLong("housekeeping.satnogs.periodMillis");
			boolean reload = currentTime - dao.getSatnogsLastUpdateTime() >= periodMillis;
			if (reload) {
				atLeastOneReloaded = true;
				dao.saveSatnogs(satnogs.loadSatellites(), currentTime);
			}
		}

		if (config.getProperty("r2cloud.apiKey") != null) {
			long periodMillis = config.getLong("housekeeping.leosatdata.periodMillis");
			boolean reload = currentTime - dao.getLeosatdataLastUpdateTime() >= periodMillis;
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
				reload = currentTime - dao.getLeosatdataNewLastUpdateTime() >= periodMillis;
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

	private void reloadTle(long currentTime) {
		long periodMillis = config.getLong("housekeeping.tle.periodMillis");
		boolean reloadTle = (currentTime - tleDao.getLastUpdateTime() >= periodMillis);
		if (!reloadTle) {
			LOG.info("Skip TLE update. Last update was {}. Next update: {}", new Date(tleDao.getLastUpdateTime()), new Date(tleDao.getLastUpdateTime() + periodMillis));
			return;
		}
		Map<String, Tle> newTle = celestrak.downloadTle();
		tleDao.saveTle(newTle, currentTime);
		dao.setTle(newTle);
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

}
