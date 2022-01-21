package ru.r2cloud.device;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.satellite.OverlappedTimetable;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.satellite.SatelliteFilter;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.ScheduledObservation;
import ru.r2cloud.satellite.SequentialTimetable;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public abstract class Device implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(Device.class);
	public static final Long PARTIAL_TOLERANCE_MILLIS = 60 * 4 * 1000L;

	private final String id;
	private final List<Satellite> scheduledSatellites = new ArrayList<>();
	private final SatelliteFilter filter;
	private final Schedule schedule;
	private final int numberOfConcurrentObservations;
	private final ThreadPoolFactory threadpoolFactory;
	private final Object sdrServerLock = new Object();
	private final Clock clock;
	private final RotatorService rotatorService;
	private final ObservationDao observationDao;
	private final DecoderService decoderService;
	private final DeviceConfiguration deviceConfiguration;

	private Long currentBandFrequency = null;
	private int numberOfObservationsOnCurrentBand = 0;
	private ScheduledExecutorService startThread = null;
	private ScheduledExecutorService stopThread = null;

	public Device(String id, SatelliteFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, ObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict) {
		this.id = id;
		this.filter = filter;
		this.numberOfConcurrentObservations = numberOfConcurrentObservations;
		this.threadpoolFactory = threadpoolFactory;
		this.clock = clock;
		if (deviceConfiguration.getRotatorConfiguration() != null) {
			this.rotatorService = new RotatorService(deviceConfiguration.getRotatorConfiguration(), predict, threadpoolFactory, clock);
		} else {
			this.rotatorService = null;
		}
		this.observationDao = observationDao;
		this.decoderService = decoderService;
		this.deviceConfiguration = deviceConfiguration;
		if (numberOfConcurrentObservations == 1) {
			this.schedule = new Schedule(new SequentialTimetable(PARTIAL_TOLERANCE_MILLIS), observationFactory);
		} else {
			this.schedule = new Schedule(new OverlappedTimetable(PARTIAL_TOLERANCE_MILLIS), observationFactory);
		}
	}

	public boolean trySatellite(Satellite satellite) {
		if (!filter.accept(satellite)) {
			return false;
		}
		scheduledSatellites.add(satellite);
		return true;
	}

	private void schedule(ObservationRequest observation, Satellite satellite) {
		// write some device-specific parameters
		observation.setGain(deviceConfiguration.getGain());
		observation.setBiast(deviceConfiguration.isBiast());
		observation.setRtlDeviceId(deviceConfiguration.getRtlDeviceId());
		observation.setPpm(deviceConfiguration.getPpm());
		observation.setSdrServerConfiguration(deviceConfiguration.getSdrServerConfiguration());
		LOG.info("scheduled next pass for {}. start: {} end: {}", satellite, new Date(observation.getStartTimeMillis()), new Date(observation.getEndTimeMillis()));
		IQReader reader = createReader(observation, satellite);
		Runnable readTask = new SafeRunnable() {

			@Override
			public void safeRun() {
				IQData data;
				// do not use lock for multiple concurrent observations
				if (numberOfConcurrentObservations > 1) {
					synchronized (sdrServerLock) {
						while (currentBandFrequency != null && currentBandFrequency != observation.getCenterBandFrequency()) {
							try {
								sdrServerLock.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
						if (clock.millis() > observation.getEndTimeMillis()) {
							LOG.info("[{}] observation time passed. skip {}", observation.getId(), satellite);
							return;
						}
						if (currentBandFrequency == null) {
							currentBandFrequency = observation.getCenterBandFrequency();
							LOG.info("starting observations on {} hz", currentBandFrequency);
						}
						numberOfObservationsOnCurrentBand++;
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
					try {
						data = reader.start();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}

				if (data == null || data.getDataFile() == null) {
					return;
				}
				// actual start/end might be different
				observation.setStartTimeMillis(data.getActualStart());
				observation.setEndTimeMillis(data.getActualEnd());

				File dataFile = observationDao.insert(observation, data.getDataFile());
				if (dataFile == null) {
					return;
				}

				synchronized (Device.this) {
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
			Future<?> rotatorFuture = null;
			if (rotatorService != null) {
				rotatorFuture = rotatorService.schedule(observation, current);
			}
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

	public void reschedule() {
		schedule.cancelAll();
		synchronized (sdrServerLock) {
			currentBandFrequency = null;
			numberOfObservationsOnCurrentBand = 0;
		}
		if (scheduledSatellites.isEmpty()) {
			LOG.info("[{}] no available satellites for this device", id);
			return;
		}
		long current = clock.millis();
		List<ObservationRequest> newSchedule = schedule.createInitialSchedule(scheduledSatellites, current);
		for (ObservationRequest cur : newSchedule) {
			Satellite fullSatelliteInfo = findById(cur.getSatelliteId());
			if (fullSatelliteInfo == null) {
				LOG.error("unable to find full satellite info for schedule observation: {}", cur.getId());
				continue;
			}
			schedule(cur, fullSatelliteInfo);
		}
		if (numberOfConcurrentObservations > 1) {
			logBandsForSdrServer(scheduledSatellites);
		}
	}

	private void logBandsForSdrServer(List<Satellite> allSatellites) {
		LOG.info("[{}] active bands are:", id);
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

	public abstract IQReader createReader(ObservationRequest req, Satellite satellite);

	@Override
	public void stop() {
		// cancel all tasks and complete active sdr readers
		schedule.cancelAll();
		if (rotatorService != null) {
			rotatorService.stop();
		}
		Util.shutdown(startThread, threadpoolFactory.getThreadPoolShutdownMillis());
		Util.shutdown(stopThread, threadpoolFactory.getThreadPoolShutdownMillis());
		startThread = null;
		LOG.info("[{}] device stopped", id);
	}

	@Override
	public void start() {
		if (startThread != null) {
			return;
		}
		startThread = threadpoolFactory.newScheduledThreadPool(numberOfConcurrentObservations, new NamingThreadFactory("sch-start"));
		stopThread = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("sch-stop"));
		if (rotatorService != null) {
			rotatorService.start();
		}
		LOG.info("[{}] device started", id);
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
		schedule(closest, satellite);
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

	public ObservationRequest findFirstBySatelliteId(String satelliteId, long current) {
		return schedule.findFirstBySatelliteId(satelliteId, current);
	}

	public ObservationRequest enableSatellite(Satellite satellite) {
		if (!trySatellite(satellite)) {
			return null;
		}
		List<ObservationRequest> batch = schedule.getBySatelliteId(satellite.getId());
		if (batch.isEmpty()) {
			batch = schedule.addSatelliteToSchedule(satellite, clock.millis());
			if (batch.isEmpty()) {
				return null;
			}

			for (ObservationRequest cur : batch) {
				schedule(cur, satellite);
			}
		}

		// return first
		Collections.sort(batch, ObservationRequestComparator.INSTANCE);
		return batch.get(0);
	}

	public void disableSatellite(Satellite satelliteToEdit) {
		if (removeSatellite(satelliteToEdit.getId())) {
			reschedule();
			LOG.info("[{}] rescheduled", id);
		}
	}

	private Satellite findById(String id) {
		for (Satellite cur : scheduledSatellites) {
			if (cur.getId().equals(id)) {
				return cur;
			}
		}
		return null;
	}

	public void removeAllSatellites() {
		scheduledSatellites.clear();
	}

	private boolean removeSatellite(String id) {
		boolean removed = false;
		Iterator<Satellite> it = scheduledSatellites.iterator();
		while (it.hasNext()) {
			Satellite cur = it.next();
			if (cur.getId().equalsIgnoreCase(id)) {
				removed = true;
				it.remove();
				break;
			}
		}
		return removed;
	}

}
