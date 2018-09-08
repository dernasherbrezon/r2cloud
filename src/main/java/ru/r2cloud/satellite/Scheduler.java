package ru.r2cloud.satellite;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.cloud.R2CloudService;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class Scheduler implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final ObservationFactory factory;
	private final SatelliteDao satellites;
	private final Configuration config;
	private final RtlSdrLock lock;
	private final ThreadPoolFactory threadpoolFactory;
	private final Clock clock;
	private final R2CloudService r2cloudService;

	private final Map<String, ScheduledObservation> scheduledObservations = new ConcurrentHashMap<String, ScheduledObservation>();

	private ScheduledExecutorService scheduler = null;
	private ScheduledExecutorService reaper = null;
	private ScheduledExecutorService decoder = null;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock, ObservationFactory factory, ThreadPoolFactory threadpoolFactory, Clock clock, R2CloudService r2cloudService) {
		this.config = config;
		this.config.subscribe(this, "satellites.enabled");
		this.config.subscribe(this, "locaiton.lat");
		this.config.subscribe(this, "locaiton.lon");
		this.satellites = satellites;
		this.lock = lock;
		this.factory = factory;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.r2cloudService = r2cloudService;
	}

	@Override
	public void onConfigUpdated() {
		List<ru.r2cloud.model.Satellite> supportedSatellites = satellites.findAll();
		for (ru.r2cloud.model.Satellite cur : supportedSatellites) {
			boolean schedule = false;
			switch (cur.getType()) {
			case WEATHER:
				if (config.getBoolean("satellites.enabled")) {
					schedule = true;
				}
				break;
			case AMATEUR:
				if (config.getProperty("locaiton.lat") != null && config.getProperty("locaiton.lon") != null) {
					schedule = true;
				}
				break;

			default:
				throw new IllegalArgumentException("type is not supported: " + cur.getType());
			}
			if (schedule) {
				schedule(cur);
			} else {
				ScheduledObservation previousObservation = scheduledObservations.get(cur.getId());
				if (previousObservation != null) {
					previousObservation.cancel();
				}
			}
		}
	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (scheduler != null) {
			return;
		}
		scheduler = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("scheduler"));
		reaper = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("reaper"));
		decoder = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
		onConfigUpdated();

		LOG.info("started");
	}

	private void schedule(ru.r2cloud.model.Satellite cur) {
		long current = clock.millis();
		Observation observation = factory.create(new Date(current), cur);
		if (observation == null) {
			return;
		}
		LOG.info("scheduled next pass for " + cur.getName() + ": " + observation.getStart());
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
		}, observation.getStart().getTime() - current, TimeUnit.MILLISECONDS);
		Future<?> reaperFuture = reaper.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				try {
					observation.stop();
				} finally {
					scheduledObservations.remove(cur.getId());
				}
				schedule(cur);
				decoder.execute(new SafeRunnable() {

					@Override
					public void doRun() {
						LOG.info("decoding: {}", cur.getName());
						observation.decode();
						LOG.info("decoded: {}", cur.getName());
						r2cloudService.uploadObservation(cur.getId(), observation.getId());
					}
				});
			}
		}, observation.getEnd().getTime() - current, TimeUnit.MILLISECONDS);
		ScheduledObservation previous = scheduledObservations.put(cur.getId(), new ScheduledObservation(observation, future, reaperFuture));
		if (previous != null) {
			LOG.info("cancelling previous: {}", previous.getObservation().getStart());
			previous.cancel();
		}
	}

	public Date getNextObservation(String id) {
		ScheduledObservation result = scheduledObservations.get(id);
		if (result == null) {
			return null;
		}
		return result.getObservation().getStart();
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
