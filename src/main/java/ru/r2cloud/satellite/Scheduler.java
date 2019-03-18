package ru.r2cloud.satellite;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.cloud.R2CloudService;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.decoder.Decoder;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.satellite.reader.RtlSdrReader;
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
	private final Schedule<ScheduledObservation> schedule;

	private ScheduledExecutorService schedulerThread = null;
	private ScheduledExecutorService reaperThread = null;
	private ScheduledExecutorService decoderThread = null;

	public Scheduler(Schedule<ScheduledObservation> schedule, Configuration config, SatelliteDao satellites, RtlSdrLock lock, ObservationFactory factory, ThreadPoolFactory threadpoolFactory, Clock clock, R2CloudService r2cloudService, ProcessFactory processFactory, ObservationResultDao dao, Map<String, Decoder> decoders) {
		this.schedule = schedule;
		this.config = config;
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
		boolean updateSchedule;
		if (config.getProperty("locaiton.lat") != null && config.getProperty("locaiton.lon") != null) {
			updateSchedule = true;
		} else {
			updateSchedule = false;
		}
		List<Satellite> supportedSatellites = satellites.findAll();
		for (Satellite cur : supportedSatellites) {
			if (!cur.isEnabled()) {
				continue;
			}
			if (updateSchedule) {
				cancel(cur);
				schedule(cur);
			} else {
				cancel(cur);
			}
		}
	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (schedulerThread != null) {
			return;
		}
		schedulerThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("scheduler"));
		reaperThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("reaper"));
		decoderThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
		onConfigUpdated();

		LOG.info("started");
	}

	public void schedule(Satellite cur) {
		long current = clock.millis();
		ObservationRequest observation = create(current, cur);
		if (observation == null) {
			return;
		}
		LOG.info("scheduled next pass for {}: {}", cur.getName(), observation.getStart().getTime());
		IQReader reader = createReader(observation);
		Future<?> future = schedulerThread.schedule(new SafeRunnable() {

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
		Future<?> reaperFuture = reaperThread.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				IQData data = reader.stop();
				schedule(cur);

				if (data == null || data.getWavFile() == null || !data.getWavFile().exists()) {
					return;
				}
				// actual start/end might be different
				observation.setStartTimeMillis(data.getActualStart());
				observation.setEndTimeMillis(data.getActualEnd());

				File wavFile = dao.insert(observation, data.getWavFile());
				if (wavFile == null) {
					return;
				}

				decoderThread.execute(new SafeRunnable() {

					@Override
					public void doRun() {
						Decoder decoder = decoders.get(observation.getSatelliteId());
						if (decoder == null) {
							LOG.error("unknown decoder: {}", decoder);
							return;
						}
						LOG.info("decoding: {}", cur.getName());
						ObservationResult result = decoder.decode(wavFile, observation);
						LOG.info("decoded: {}", cur.getName());

						if (result.getDataPath() != null) {
							result.setDataPath(dao.saveData(observation.getSatelliteId(), observation.getId(), result.getDataPath()));
						}
						if (result.getaPath() != null) {
							result.setaPath(dao.saveImage(observation.getSatelliteId(), observation.getId(), result.getaPath()));
						}

						ObservationFull full = dao.find(observation.getSatelliteId(), observation.getId());
						full.setResult(result);
						dao.update(full);
						r2cloudService.uploadObservation(full);
					}
				});
			}
		}, observation.getEndTimeMillis() - current, TimeUnit.MILLISECONDS);
		schedule.add(new ScheduledObservation(observation, future, reaperFuture));
	}

	private ObservationRequest create(long current, Satellite cur) {
		long next = current;
		while (!Thread.currentThread().isInterrupted()) {
			ObservationRequest observation = factory.create(new Date(next), cur);
			if (observation == null) {
				return null;
			}

			if (!schedule.hasOverlap(observation.getStartTimeMillis(), observation.getEndTimeMillis())) {
				return observation;
			}

			// find next
			next = observation.getEndTimeMillis();
		}
		return null;
	}

	private IQReader createReader(ObservationRequest req) {
		FrequencySource source = req.getSource();
		switch (source) {
		case APT:
			return new RtlFmReader(config, processFactory, req);
		case LRPT:
		case TELEMETRY:
			return new RtlSdrReader(config, processFactory, req);
		default:
			throw new IllegalArgumentException("unsupported source: " + source);
		}
	}

	public ScheduleEntry getNextObservation(String id) {
		return schedule.get(id);
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		Util.shutdown(schedulerThread, config.getThreadPoolShutdownMillis());
		Util.shutdown(reaperThread, config.getThreadPoolShutdownMillis());
		schedulerThread = null;
		LOG.info("stopped");
	}

	public void cancel(Satellite satelliteToEdit) {
		schedule.cancel(satelliteToEdit.getId());
	}

}
