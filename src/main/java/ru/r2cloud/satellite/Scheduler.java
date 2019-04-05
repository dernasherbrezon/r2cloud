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

	private ScheduledExecutorService startThread = null;
	private ScheduledExecutorService stopThread = null;
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
		if (startThread == null) {
			return;
		}

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
				schedule(cur, false);
			} else {
				cancel(cur);
			}
		}
	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (startThread != null) {
			return;
		}
		startThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("sch-start"));
		stopThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("sch-stop"));
		decoderThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("decoder"));
		onConfigUpdated();

		LOG.info("started");
	}

	public ObservationRequest schedule(Satellite cur, boolean immediately) {
		long current = clock.millis();
		ObservationRequest observation = create(current, cur, immediately);
		if (observation == null) {
			return null;
		}
		LOG.info("scheduled next pass for {}. start: {} end: {}", cur.getName(), new Date(observation.getStartTimeMillis()), new Date(observation.getEndTimeMillis()));
		IQReader reader = createReader(observation);
		SafeRunnable readTask = new SafeRunnable() {

			@Override
			public void doRun() {
				if (clock.millis() > observation.getEndTimeMillis()) {
					LOG.info("[{}] observation time passed. skip {}", observation.getId(), cur.getName());
					return;
				}
				if (!lock.tryLock(Scheduler.this)) {
					LOG.info("[{}] unable to acquire lock for {}", observation.getId(), cur.getName());
					return;
				}
				IQData data;
				try {
					data = reader.start();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} finally {
					lock.unlock(Scheduler.this);
				}

				schedule(cur, false);

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
							LOG.error("[{}] unknown decoder for {}", observation.getId(), observation.getSatelliteId());
							return;
						}
						LOG.info("[{}] decoding", observation.getId());
						ObservationResult result = decoder.decode(wavFile, observation);
						LOG.info("[{}] decoded", observation.getId());

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
		};
		Future<?> future = startThread.schedule(readTask, observation.getStartTimeMillis() - current, TimeUnit.MILLISECONDS);
		SafeRunnable completeTask = new SafeRunnable() {

			@Override
			public void doRun() {
				reader.complete();
			}
		};
		Future<?> reaperFuture = stopThread.schedule(completeTask, observation.getEndTimeMillis() - current, TimeUnit.MILLISECONDS);
		schedule.add(new ScheduledObservation(observation, future, reaperFuture, readTask, completeTask));
		return observation;
	}

	private ObservationRequest create(long current, Satellite cur, boolean immediately) {
		long next = current;
		while (!Thread.currentThread().isInterrupted()) {
			ObservationRequest observation = factory.create(new Date(next), cur, immediately);
			if (observation == null) {
				return null;
			}

			ScheduledObservation overlapped = schedule.getOverlap(observation.getStartTimeMillis(), observation.getEndTimeMillis());
			if (overlapped == null) {
				return observation;
			}

			if (immediately) {
				overlapped.cancel();
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
		Util.shutdown(startThread, config.getThreadPoolShutdownMillis());
		Util.shutdown(stopThread, config.getThreadPoolShutdownMillis());
		startThread = null;
		LOG.info("stopped");
	}

	public void cancel(Satellite satelliteToEdit) {
		schedule.cancel(satelliteToEdit.getId());
	}

	public ObservationRequest startImmediately(Satellite cur) {
		schedule.cancel(cur.getId());
		return schedule(cur, true);
	}

	public void completeImmediately(String id) {
		ScheduledObservation previous = schedule.cancel(id);
		if (previous == null) {
			return;
		}
		stopThread.submit(previous.getCompleteTask());
	}

}
