package ru.r2cloud.satellite;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;

public class Scheduler implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final ObservationFactory factory;
	private final SatelliteDao satellites;
	private final Configuration config;
	private final RtlSdrLock lock;

	private final Map<String, Date> scheduledObservations = new ConcurrentHashMap<String, Date>();

	private ScheduledExecutorService scheduler = null;
	private ScheduledExecutorService reaper = null;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock, ObservationFactory factory) {
		this.config = config;
		this.config.subscribe(this, "satellites.enabled");
		this.satellites = satellites;
		this.lock = lock;
		this.factory = factory;
	}

	@Override
	public void onConfigUpdated() {
		boolean satellitesEnabled = config.getBoolean("satellites.enabled");
		if (scheduler == null && satellitesEnabled) {
			start();
		} else if (scheduler != null && !satellitesEnabled) {
			stop();
		}
	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (!config.getBoolean("satellites.enabled")) {
			LOG.info("satellite scheduler is disabled");
			return;
		}
		if (scheduler != null) {
			return;
		}
		List<ru.r2cloud.model.Satellite> supportedSatellites = satellites.findSupported();
		scheduler = Executors.newScheduledThreadPool(1, new NamingThreadFactory("scheduler"));
		reaper = Executors.newScheduledThreadPool(1, new NamingThreadFactory("reaper"));
		for (ru.r2cloud.model.Satellite cur : supportedSatellites) {
			schedule(cur);
		}

		LOG.info("started");
	}

	private void schedule(ru.r2cloud.model.Satellite cur) {
		long current = System.currentTimeMillis();
		Observation observation = factory.create(new Date(current), cur);
		if (observation == null) {
			return;
		}
		LOG.info("scheduled next pass for " + cur.getName() + ": " + observation.getNextPass());
		scheduledObservations.put(cur.getId(), observation.getNextPass().getStart().getTime());
		Future<?> future = scheduler.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				if (!lock.tryLock(Scheduler.this)) {
					LOG.info("unable to acquire lock for " + cur.getName());
					return;
				}
				try {
					observation.start();
				} finally {
					lock.unlock(Scheduler.this);
				}
			}
		}, observation.getNextPass().getStart().getTime().getTime() - current, TimeUnit.MILLISECONDS);
		reaper.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				observation.stop();
				schedule(cur);
			}
		}, observation.getNextPass().getEnd().getTime().getTime() - current, TimeUnit.MILLISECONDS);
	}

	public Date getNextObservation(String id) {
		return scheduledObservations.get(id);
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		Util.shutdown(scheduler, config.getThreadPoolShutdownMillis());
		Util.shutdown(reaper, config.getThreadPoolShutdownMillis());
		scheduler = null;
		LOG.info("stopped");
	}

}
