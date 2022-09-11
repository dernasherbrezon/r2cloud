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
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.RotatorStatus;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.satellite.OverlappedTimetable;
import ru.r2cloud.satellite.RotatorService;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.ScheduledObservation;
import ru.r2cloud.satellite.SequentialTimetable;
import ru.r2cloud.satellite.TransmitterFilter;
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
	private final List<Transmitter> scheduledTransmitters = new ArrayList<>();
	private final TransmitterFilter filter;
	private final Schedule schedule;
	private final int numberOfConcurrentObservations;
	private final ThreadPoolFactory threadpoolFactory;
	private final Object sdrServerLock = new Object();
	private final Clock clock;
	private final RotatorService rotatorService;
	private final IObservationDao observationDao;
	private final DecoderService decoderService;
	private final DeviceConfiguration deviceConfiguration;

	private Long currentBandFrequency = null;
	private int numberOfObservationsOnCurrentBand = 0;
	private ScheduledExecutorService startThread = null;
	private ScheduledExecutorService stopThread = null;

	protected Device(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule) {
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
		if (schedule != null) {
			this.schedule = schedule;
		} else {
			if (numberOfConcurrentObservations == 1) {
				this.schedule = new Schedule(new SequentialTimetable(PARTIAL_TOLERANCE_MILLIS), observationFactory);
			} else {
				this.schedule = new Schedule(new OverlappedTimetable(PARTIAL_TOLERANCE_MILLIS), observationFactory);
			}
		}
	}

	public synchronized boolean tryTransmitter(Transmitter transmitter) {
		if (!filter.accept(transmitter)) {
			return false;
		}
		scheduledTransmitters.add(transmitter);
		return true;
	}

	private void schedule(ObservationRequest req, Transmitter transmitter) {
		// write some device-specific parameters
		req.setGain(deviceConfiguration.getGain());
		req.setBiast(deviceConfiguration.isBiast());
		req.setRtlDeviceId(deviceConfiguration.getRtlDeviceId());
		req.setPpm(deviceConfiguration.getPpm());
		req.setSdrServerConfiguration(deviceConfiguration.getSdrServerConfiguration());
		req.setSampleRate(transmitter.getInputSampleRate());
		LOG.info("[{}] scheduled next pass for {}. start: {} end: {}", id, transmitter, new Date(req.getStartTimeMillis()), new Date(req.getEndTimeMillis()));
		IQReader reader = createReader(req, transmitter);
		Runnable readTask = new SafeRunnable() {

			@Override
			public void safeRun() {
				IQData data;
				Observation observation = new Observation(req);
				observation.setStatus(ObservationStatus.RECEIVING_DATA);
				// do not use lock for multiple concurrent observations
				if (numberOfConcurrentObservations > 1) {
					synchronized (sdrServerLock) {
						while (currentBandFrequency != null && currentBandFrequency != req.getCenterBandFrequency()) {
							try {
								sdrServerLock.wait();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
						if (clock.millis() > req.getEndTimeMillis()) {
							LOG.info("[{}] observation time passed. skip {}", req.getId(), transmitter);
							return;
						}
						if (currentBandFrequency == null) {
							currentBandFrequency = req.getCenterBandFrequency();
							LOG.info("starting observations on {} hz", currentBandFrequency);
						}
						numberOfObservationsOnCurrentBand++;
					}
					// insert only when it actually started on the device
					observationDao.insert(observation);
					try {
						data = reader.start();
					} catch (InterruptedException e) {
						LOG.info("[{}] reader was terminated", req.getId());
						observationDao.cancel(observation);
						Thread.currentThread().interrupt();
						return;
					} finally {
						synchronized (sdrServerLock) {
							numberOfObservationsOnCurrentBand--;
							if (numberOfObservationsOnCurrentBand <= 0) {
								LOG.info("no more observations on: {} hz", currentBandFrequency);
								currentBandFrequency = null;
							}
							sdrServerLock.notifyAll();
						}
					}
				} else {
					if (clock.millis() > req.getEndTimeMillis()) {
						LOG.info("[{}] observation time passed. skip {}", req.getId(), transmitter);
						return;
					}
					observationDao.insert(observation);
					try {
						data = reader.start();
					} catch (InterruptedException e) {
						observationDao.cancel(observation);
						Thread.currentThread().interrupt();
						return;
					}
				}

				if (data == null || data.getDataFile() == null) {
					observationDao.cancel(observation);
					return;
				}
				// actual start/end might be different
				observation.setStartTimeMillis(data.getActualStart());
				observation.setEndTimeMillis(data.getActualEnd());
				observation.setStatus(ObservationStatus.RECEIVED);

				File dataFile = observationDao.update(observation, data.getDataFile());
				if (dataFile == null) {
					return;
				}

				synchronized (Device.this) {
					if (startThread == null) {
						return;
					}

					decoderService.run(dataFile, req);
				}

			}
		};
		synchronized (this) {
			if (startThread == null) {
				return;
			}
			long current = clock.millis();
			Future<?> startFuture = startThread.schedule(readTask, req.getStartTimeMillis() - current, TimeUnit.MILLISECONDS);
			Future<?> rotatorFuture = null;
			if (rotatorService != null) {
				rotatorFuture = rotatorService.schedule(req, current, startFuture);
			}
			Runnable completeTask = new SafeRunnable() {

				@Override
				public void safeRun() {
					reader.complete();
				}
			};
			Future<?> stopRtlSdrFuture = stopThread.schedule(completeTask, req.getEndTimeMillis() - current, TimeUnit.MILLISECONDS);
			schedule.assignTasksToSlot(req.getId(), new ScheduledObservation(startFuture, stopRtlSdrFuture, completeTask, rotatorFuture));
		}
	}

	public void reschedule() {
		synchronized (sdrServerLock) {
			currentBandFrequency = null;
			numberOfObservationsOnCurrentBand = 0;
		}
		synchronized (this) {
			if (scheduledTransmitters.isEmpty()) {
				LOG.info("[{}] no available satellites for this device", id);
				return;
			}
			long current = clock.millis();
			List<ObservationRequest> newSchedule = schedule.createInitialSchedule(scheduledTransmitters, current);
			for (ObservationRequest cur : newSchedule) {
				Transmitter fullSatelliteInfo = findById(cur.getTransmitterId());
				if (fullSatelliteInfo == null) {
					LOG.error("unable to find full transmitter info for schedule observation: {}", cur.getId());
					continue;
				}
				schedule(cur, fullSatelliteInfo);
			}
			if (numberOfConcurrentObservations > 1) {
				logBandsForSdrServer(scheduledTransmitters);
			}
		}
	}

	private void logBandsForSdrServer(List<Transmitter> allSatellites) {
		LOG.info("[{}] active bands are:", id);
		Set<BandFrequency> unique = new HashSet<>();
		for (Transmitter cur : allSatellites) {
			unique.add(cur.getFrequencyBand());
		}
		List<BandFrequency> sorted = new ArrayList<>(unique);
		Collections.sort(sorted, BandFrequencyComparator.INSTANCE);
		for (BandFrequency cur : sorted) {
			LOG.info("  {} - {}", cur.getLower(), cur.getUpper());
		}
	}

	public abstract IQReader createReader(ObservationRequest req, Transmitter satellite);

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

	public ObservationRequest startImmediately(Transmitter transmitter) {
		long startTime = clock.millis();
		ObservationRequest closest = schedule.findFirstByTransmitterId(transmitter.getId(), startTime);
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
		schedule(closest, transmitter);
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

	public ObservationRequest findFirstByTransmitter(Transmitter transmitter) {
		// schedule can be shared. thus containing observations from different
		// transmitters
		if (!filter.accept(transmitter)) {
			return null;
		}
		return schedule.findFirstByTransmitterId(transmitter.getId(), clock.millis());
	}

	public ObservationRequest enableTransmitter(Transmitter transmitter) {
		if (!tryTransmitter(transmitter)) {
			return null;
		}
		List<ObservationRequest> batch = schedule.getByTransmitterId(transmitter.getId());
		if (batch.isEmpty()) {
			batch = schedule.addToSchedule(transmitter, clock.millis());
			if (batch.isEmpty()) {
				return null;
			}

			for (ObservationRequest cur : batch) {
				schedule(cur, transmitter);
			}
		}

		// return first
		Collections.sort(batch, ObservationRequestComparator.INSTANCE);
		return batch.get(0);
	}

	public List<ObservationRequest> findScheduledObservations() {
		List<ObservationRequest> result = new ArrayList<>();
		for (Transmitter cur : scheduledTransmitters) {
			result.addAll(schedule.getByTransmitterId(cur.getId()));
		}
		return result;
	}

	public synchronized void disableTransmitter(Transmitter transmitter) {
		if (removeTransmitter(transmitter.getId())) {
			reschedule();
			LOG.info("[{}] rescheduled", id);
		}
	}

	private Transmitter findById(String id) {
		for (Transmitter cur : scheduledTransmitters) {
			if (cur.getId().equals(id)) {
				return cur;
			}
		}
		return null;
	}

	public synchronized void removeAllTransmitters() {
		for (Transmitter cur : scheduledTransmitters) {
			schedule.cancelByTransmitter(cur.getId());
		}
		scheduledTransmitters.clear();
	}

	private boolean removeTransmitter(String id) {
		boolean removed = false;
		Iterator<Transmitter> it = scheduledTransmitters.iterator();
		while (it.hasNext()) {
			Transmitter cur = it.next();
			if (cur.getId().equalsIgnoreCase(id)) {
				removed = true;
				it.remove();
				break;
			}
		}
		return removed;
	}

	public DeviceStatus getStatus() {
		DeviceStatus result = new DeviceStatus();
		result.setConfig(deviceConfiguration);
		if (rotatorService != null) {
			result.setRotatorStatus(rotatorService.getStatus());
		} else {
			RotatorStatus rotatorStatus = new RotatorStatus();
			rotatorStatus.setStatus(DeviceConnectionStatus.DISABLED);
			result.setRotatorStatus(rotatorStatus);
		}
		return result;
	}

}
