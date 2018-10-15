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
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ProcessFactory;
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
	private final ProcessFactory processFactory;
	private final ObservationResultDao dao;
	private final Map<String, Decoder> decoders;

	private final Map<String, ScheduledObservation> scheduledObservations = new ConcurrentHashMap<String, ScheduledObservation>();

	private ScheduledExecutorService scheduler = null;
	private ScheduledExecutorService reaper = null;
	private ScheduledExecutorService decoder = null;

	public Scheduler(Configuration config, SatelliteDao satellites, RtlSdrLock lock, ObservationFactory factory, ThreadPoolFactory threadpoolFactory, Clock clock, R2CloudService r2cloudService, ProcessFactory processFactory, ObservationResultDao dao, Map<String, Decoder> decoders) {
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
		this.processFactory = processFactory;
		this.dao = dao;
		this.decoders = decoders;
	}

	@Override
	public void onConfigUpdated() {
		List<Satellite> supportedSatellites = satellites.findAll();
		for (Satellite cur : supportedSatellites) {
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

	private void schedule(Satellite cur) {
		long current = clock.millis();
		ObservationRequest observation = factory.create(new Date(current), cur);
		if (observation == null) {
			return;
		}
		LOG.info("scheduled next pass for {}: {}", cur.getName(), observation.getStart().getTime());
		IQReader reader = createReader(observation);
		Future<?> future = scheduler.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				if (!lock.tryLock(Scheduler.this)) {
					LOG.info("unable to acquire lock for {}", cur.getName());
					return;
				}
				try {
					reader.start();
				} finally {
					lock.unlock(Scheduler.this);
				}
			}
		}, observation.getStartTimeMillis() - current, TimeUnit.MILLISECONDS);
		Future<?> reaperFuture = reaper.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				IQData data;
				future.cancel(true);
				try {
					data = reader.stop();
				} finally {
					scheduledObservations.remove(cur.getId());
				}
				schedule(cur);

				if (data == null || data.getWavFile() == null || !data.getWavFile().exists()) {
					return;
				}

				ObservationFull full = new ObservationFull();
				full.setReq(observation);

				if (!dao.insert(full, data.getWavFile())) {
					return;
				}

				observation.setStartTimeMillis(data.getActualStart());
				observation.setEndTimeMillis(data.getActualEnd());

				decoder.execute(new SafeRunnable() {

					@Override
					public void doRun() {
						Decoder decoder = decoders.get(observation.getDecoder());
						if (decoder == null) {
							LOG.error("unknown decoder: {}", decoder);
							return;
						}
						LOG.info("decoding: {}", cur.getName());
						ObservationResult result = decoder.decode(data.getWavFile(), observation);
						LOG.info("decoded: {}", cur.getName());

						if (result.getDataPath() != null) {
							dao.saveData(observation.getSatelliteId(), observation.getId(), result.getDataPath());
						}
						if (result.getaPath() != null) {
							dao.saveImage(observation.getSatelliteId(), observation.getId(), result.getaPath(), "a");
						}
						full.setResult(result);
						dao.update(full);
						r2cloudService.uploadObservation(full);
					}
				});
			}
		}, observation.getEndTimeMillis() - current, TimeUnit.MILLISECONDS);
		ScheduledObservation previous = scheduledObservations.put(cur.getId(), new ScheduledObservation(observation, future, reaperFuture));
		if (previous != null) {
			LOG.info("cancelling previous: {}", previous.getReq().getStart().getTime());
			previous.cancel();
		}
	}

	private IQReader createReader(ObservationRequest req) {
		String decoder = req.getDecoder();
		if (decoder.equals("apt")) {
			return new RtlFmReader(config, processFactory, req);
		} else if (decoder.equals("lrpt") || decoder.equals("aausat4")) {
			return new RtlSdrReader(config, processFactory, req);
		} else {
			throw new IllegalArgumentException("unsupported decoder: " + decoder);
		}
	}

	public Long getNextObservation(String id) {
		ScheduledObservation result = scheduledObservations.get(id);
		if (result == null) {
			return null;
		}
		return result.getReq().getStartTimeMillis();
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
