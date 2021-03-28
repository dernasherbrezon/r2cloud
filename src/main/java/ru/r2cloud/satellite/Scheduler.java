package ru.r2cloud.satellite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.BandFrequencyComparator;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.PlutoSdrReader;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.satellite.reader.RtlSdrReader;
import ru.r2cloud.satellite.reader.SdrServerReader;
import ru.r2cloud.sdr.SdrLock;
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

	private final SatelliteDao satelliteDao;
	private final Configuration config;
	private final SdrLock lock;
	private final ThreadPoolFactory threadpoolFactory;
	private final Clock clock;
	private final ProcessFactory processFactory;
	private final ObservationDao dao;
	private final DecoderService decoderService;
	private final Schedule schedule;
	private final RotatorService rotatorService;

	private ScheduledExecutorService startThread = null;
	private ScheduledExecutorService stopThread = null;
	private ScheduledExecutorService rescheduleThread = null;
	private Future<?> rescheduleTask;

	private final Object sdrServerLock = new Object();
	private Long currentBandFrequency = null;
	private int numberOfObservationsOnCurrentBand = 0;

	public Scheduler(Schedule schedule, Configuration config, SatelliteDao satellites, SdrLock lock, ThreadPoolFactory threadpoolFactory, Clock clock, ProcessFactory processFactory, ObservationDao dao, DecoderService decoderService, RotatorService rotatorService) {
		this.schedule = schedule;
		this.config = config;
		this.config.subscribe(this, "locaiton.lat");
		this.config.subscribe(this, "locaiton.lon");
		this.satelliteDao = satellites;
		this.lock = lock;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		this.processFactory = processFactory;
		this.dao = dao;
		this.decoderService = decoderService;
		this.rotatorService = rotatorService;
	}

	@Override
	public void onConfigUpdated() {
		if (startThread == null) {
			return;
		}

		if (config.getProperty("locaiton.lat") != null && config.getProperty("locaiton.lon") != null) {
			reschedule();
		} else {
			LOG.info("missing location. cancelling all observations");
			schedule.cancelAll();
		}
	}

	// protection from calling start 2 times and more
	@Override
	public synchronized void start() {
		if (startThread != null) {
			return;
		}
		int numberOfConcurrentObservations = 1;
		if (config.getSdrType().equals(SdrType.SDRSERVER) && !config.getBoolean("rotator.enabled")) {
			numberOfConcurrentObservations = 5;
		}
		startThread = threadpoolFactory.newScheduledThreadPool(numberOfConcurrentObservations, new NamingThreadFactory("sch-start"));
		stopThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("sch-stop"));
		rescheduleThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("re-schedule"));
		onConfigUpdated();

		LOG.info("started");
	}

	public ObservationRequest schedule(Satellite satellite) {
		List<ObservationRequest> batch = schedule.getBySatelliteId(satellite.getId());
		if (batch.isEmpty()) {
			batch = schedule.addSatelliteToSchedule(satellite, clock.millis());
			if (batch.isEmpty()) {
				return null;
			}

			for (ObservationRequest cur : batch) {
				schedule(cur);
			}
		}

		// return first
		Collections.sort(batch, ObservationRequestComparator.INSTANCE);
		return batch.get(0);
	}

	private void schedule(ObservationRequest observation) {
		Satellite satellite = satelliteDao.findById(observation.getSatelliteId());
		LOG.info("scheduled next pass for {}. start: {} end: {}", satellite, new Date(observation.getStartTimeMillis()), new Date(observation.getEndTimeMillis()));
		IQReader reader = createReader(observation);
		Runnable readTask = new SafeRunnable() {

			@Override
			public void safeRun() {
				IQData data;
				// do not use lock for multiple concurrent observations
				if (config.getSdrType().equals(SdrType.SDRSERVER)) {
					synchronized (sdrServerLock) {
						while (currentBandFrequency != null && currentBandFrequency != observation.getCenterBandFrequency()) {
							try {
								sdrServerLock.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
						if (currentBandFrequency == null) {
							currentBandFrequency = observation.getCenterBandFrequency();
							LOG.info("starting observations on {} hz", currentBandFrequency);
						}
						numberOfObservationsOnCurrentBand++;
					}
					if (clock.millis() > observation.getEndTimeMillis()) {
						LOG.info("[{}] observation time passed. skip {}", observation.getId(), satellite);
						return;
					}
					try {
						data = reader.start();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					synchronized (sdrServerLock) {
						numberOfObservationsOnCurrentBand--;
						if (numberOfObservationsOnCurrentBand == 0) {
							LOG.info("no more observations on: {} hz", currentBandFrequency);
							currentBandFrequency = null;
						}
						sdrServerLock.notifyAll();
					}
				} else {
					if (clock.millis() > observation.getEndTimeMillis()) {
						LOG.info("[{}] observation time passed. skip {}", observation.getId(), satellite);
						return;
					}
					if (!lock.tryLock(Scheduler.this)) {
						LOG.info("[{}] unable to acquire lock for {}", observation.getId(), satellite);
						return;
					}
					try {
						data = reader.start();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					} finally {
						lock.unlock(Scheduler.this);
					}
				}

				if (data == null || data.getDataFile() == null) {
					return;
				}
				// actual start/end might be different
				observation.setStartTimeMillis(data.getActualStart());
				observation.setEndTimeMillis(data.getActualEnd());

				File dataFile = dao.insert(observation, data.getDataFile());
				if (dataFile == null) {
					return;
				}

				synchronized (Scheduler.this) {
					if (startThread == null) {
						return;
					}

					decoderService.run(dataFile, observation);
				}

			}
		};
		synchronized (this) {
			if (startThread == null) {
				return;
			}
			long current = clock.millis();
			Future<?> startFuture = startThread.schedule(readTask, observation.getStartTimeMillis() - current, TimeUnit.MILLISECONDS);
			Future<?> rotatorFuture = rotatorService.schedule(observation, current);
			Runnable completeTask = new SafeRunnable() {

				@Override
				public void safeRun() {
					reader.complete();
				}
			};
			Future<?> stopRtlSdrFuture = stopThread.schedule(completeTask, observation.getEndTimeMillis() - current, TimeUnit.MILLISECONDS);
			schedule.assignTasksToSlot(observation.getId(), new ScheduledObservation(startFuture, stopRtlSdrFuture, completeTask, rotatorFuture));
		}
	}

	private IQReader createReader(ObservationRequest req) {
		FrequencySource source = req.getSource();
		switch (source) {
		case APT:
			return new RtlFmReader(config, processFactory, req);
		case LRPT:
		case TELEMETRY:
		case FSK_AX25_G3RUH:
			if (req.getSdrType().equals(SdrType.RTLSDR)) {
				return new RtlSdrReader(config, processFactory, req);
			} else if (req.getSdrType().equals(SdrType.PLUTOSDR)) {
				return new PlutoSdrReader(config, processFactory, req);
			} else if (req.getSdrType().equals(SdrType.SDRSERVER)) {
				return new SdrServerReader(config, req);
			} else {
				throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
			}
		default:
			throw new IllegalArgumentException("unsupported source: " + source);
		}
	}

	// protection from calling stop 2 times and more
	@Override
	public synchronized void stop() {
		// cancel all tasks and complete active sdr readers
		schedule.cancelAll();
		Util.shutdown(startThread, config.getThreadPoolShutdownMillis());
		Util.shutdown(stopThread, config.getThreadPoolShutdownMillis());
		Util.shutdown(rescheduleThread, config.getThreadPoolShutdownMillis());
		startThread = null;
		LOG.info("stopped");
	}

	public void reschedule() {
		schedule.cancelAll();
		List<Satellite> allSatellites = satelliteDao.findEnabled();
		long current = clock.millis();
		List<ObservationRequest> newSchedule = schedule.createInitialSchedule(allSatellites, current);
		for (ObservationRequest cur : newSchedule) {
			schedule(cur);
		}
		long delay;
		if (!newSchedule.isEmpty()) {
			delay = newSchedule.get(newSchedule.size() - 1).getEndTimeMillis() + 1000 - current;
		} else {
			delay = TimeUnit.DAYS.toMillis(2);
		}
		LOG.info("observations rescheduled. next update at: {}", new Date(current + delay));
		synchronized (this) {
			if (rescheduleTask != null) {
				rescheduleTask.cancel(true);
			}
			rescheduleTask = rescheduleThread.schedule(new SafeRunnable() {

				@Override
				public void safeRun() {
					reschedule();
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
		if (config.getSdrType().equals(SdrType.SDRSERVER)) {
			logBandsForSdrServer(allSatellites);
		}
	}

	private static void logBandsForSdrServer(List<Satellite> allSatellites) {
		LOG.info("active bands are:");
		Set<BandFrequency> unique = new HashSet<>();
		for (Satellite cur : allSatellites) {
			unique.add(cur.getFrequencyBand());
		}
		List<BandFrequency> sorted = new ArrayList<>(unique);
		Collections.sort(sorted, BandFrequencyComparator.INSTANCE);
		for (BandFrequency cur : sorted) {
			LOG.info("  {} - {}", cur.getLower(), cur.getUpper());
		}
	}

	public ObservationRequest startImmediately(Satellite satellite) {
		long startTime = clock.millis();
		ObservationRequest closest = schedule.findFirstBySatelliteId(satellite.getId(), startTime);
		if (closest == null) {
			return null;
		}
		long endTime = startTime + (closest.getEndTimeMillis() - closest.getStartTimeMillis());
		List<ObservationRequest> requests = schedule.findObservations(startTime, endTime);
		for (ObservationRequest cur : requests) {
			completeImmediately(cur.getId());
		}
		ObservationRequest movedTo = schedule.moveObservation(closest, startTime);
		if (movedTo == null) {
			return null;
		}
		schedule(closest);
		return closest;
	}

	public boolean completeImmediately(String observationId) {
		ScheduledObservation previous = schedule.cancel(observationId);
		if (previous == null) {
			return false;
		}
		stopThread.submit(previous.getCompleteTask());
		return true;
	}

}
