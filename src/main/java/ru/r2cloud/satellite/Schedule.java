package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.util.Configuration;

public class Schedule {

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);
	private static final Long partialToleranceMillis = 60 * 4 * 1000L;

	private final ObservationFactory factory;
	private final Timetable timetable;

	private final Map<String, ScheduledObservation> tasksById = new HashMap<>();
	private final Map<String, ObservationRequest> observationsById = new HashMap<>();
	private final Map<String, TimeSlot> timeSlotById = new HashMap<>();
	private final Map<String, List<ObservationRequest>> observationsBySatelliteId = new HashMap<>();

	public Schedule(Configuration config, ObservationFactory factory) {
		this.factory = factory;
		boolean rotatorIsEnabled = config.getBoolean("rotator.enabled");
		// this complicated if just to put some logging
		if (config.getSdrType().equals(SdrType.SDRSERVER)) {
			if (rotatorIsEnabled) {
				LOG.info("concurrent observations are disabled because of rotator");
				timetable = new SequentialTimetable(partialToleranceMillis);
			} else {
				timetable = new OverlappedTimetable(partialToleranceMillis);
			}
		} else {
			timetable = new SequentialTimetable(partialToleranceMillis);
		}

	}

	public synchronized void assignTasksToSlot(String observationId, ScheduledObservation entry) {
		if (!observationsById.containsKey(observationId)) {
			throw new IllegalArgumentException("unknown observation");
		}
		ScheduledObservation previous = tasksById.put(observationId, entry);
		if (previous != null && previous != entry) {
			LOG.info("cancelling previous scheduled tasks for observation: {}", observationId);
			previous.cancel();
		}
	}

	// scheduled observation might not exist
	public synchronized ScheduledObservation cancel(String observationId) {
		if (observationId == null) {
			return null;
		}
		ObservationRequest previousReq = observationsById.remove(observationId);
		if (previousReq == null) {
			return null;
		}
		removeFromIndex(previousReq);
		timetable.remove(timeSlotById.remove(observationId));
		ScheduledObservation previous = tasksById.remove(observationId);
		if (previous == null) {
			return null;
		}
		LOG.info("cancelling {}: {}", observationId, previousReq.getStartTimeMillis());
		previous.cancel();
		return previous;
	}

	public synchronized void cancelAll() {
		for (ObservationRequest cur : observationsById.values()) {
			removeFromIndex(cur);
			timetable.remove(timeSlotById.remove(cur.getId()));
			ScheduledObservation previous = tasksById.remove(cur.getId());
			if (previous == null) {
				continue;
			}
			previous.cancel();
		}
		observationsById.clear();
	}

	public synchronized List<ObservationRequest> createInitialSchedule(List<Satellite> allSatellites, long current) {
		Map<String, List<ObservationRequest>> passesBySatellite = new HashMap<>();
		for (Satellite cur : allSatellites) {
			List<ObservationRequest> passes = factory.createSchedule(new Date(current), cur);
			if (passes.isEmpty()) {
				continue;
			}
			passesBySatellite.put(cur.getId(), passes);
		}

		List<ObservationRequest> result = new ArrayList<>();

		// fill-in full observations
		while (!Thread.currentThread().isInterrupted()) {
			boolean moreObservationsToCheck = false;
			for (Satellite cur : allSatellites) {
				List<ObservationRequest> allPasses = passesBySatellite.get(cur.getId());
				if (allPasses == null) {
					continue;
				}
				ObservationRequest reserved = reserveFullSlot(allPasses);
				if (reserved == null) {
					continue;
				}
				moreObservationsToCheck = true;
				result.add(reserved);
				allPasses.remove(reserved);
			}

			if (!moreObservationsToCheck) {
				break;
			}
		}

		// fill-in partial observations
		while (!Thread.currentThread().isInterrupted()) {
			boolean moreObservationsToCheck = false;
			for (Satellite cur : allSatellites) {
				List<ObservationRequest> allPasses = passesBySatellite.get(cur.getId());
				if (allPasses == null) {
					continue;
				}
				ObservationRequest reserved = reservePartialSlot(allPasses);
				if (reserved == null) {
					continue;
				}
				moreObservationsToCheck = true;
				result.add(reserved);
				allPasses.remove(reserved);
			}

			if (!moreObservationsToCheck) {
				break;
			}
		}

		index(result);
		Collections.sort(result, ObservationRequestComparator.INSTANCE);
		return result;
	}

	public synchronized List<ObservationRequest> getBySatelliteId(String satelliteId) {
		List<ObservationRequest> previous = observationsBySatelliteId.get(satelliteId);
		if (previous == null) {
			return Collections.emptyList();
		}
		return previous;
	}

	// even if this satellite will be scheduled well forward the rest of satellites
	// whole schedule will be cleared on next re-schedule
	public synchronized List<ObservationRequest> addSatelliteToSchedule(Satellite satellite, long current) {
		List<ObservationRequest> previous = observationsBySatelliteId.get(satellite.getId());
		if (previous != null && !previous.isEmpty()) {
			return previous;
		}
		List<ObservationRequest> allPasses = factory.createSchedule(new Date(current), satellite);
		List<ObservationRequest> batch = new ArrayList<>();
		for (ObservationRequest cur : allPasses) {
			TimeSlot slot = new TimeSlot();
			slot.setStart(cur.getStartTimeMillis());
			slot.setEnd(cur.getEndTimeMillis());
			slot.setFrequency(satellite.getFrequencyBand().getCenter());
			if (timetable.addFully(slot)) {
				batch.add(cur);
				timeSlotById.put(cur.getId(), slot);
				continue;
			}
			TimeSlot partial = timetable.addPartially(slot);
			if (partial != null) {
				cur.setStartTimeMillis(partial.getStart());
				cur.setEndTimeMillis(partial.getEnd());
				batch.add(cur);
				timeSlotById.put(cur.getId(), partial);
				continue;
			}
		}
		index(batch);
		return batch;
	}

	public synchronized ObservationRequest findFirstBySatelliteId(String id, long current) {
		List<ObservationRequest> curList = observationsBySatelliteId.get(id);
		if (curList == null || curList.isEmpty()) {
			return null;
		}
		for (ObservationRequest cur : curList) {
			// the list is sorted so it is safe to do that
			if (cur.getStartTimeMillis() > current) {
				return cur;
			}
		}
		return null;
	}

	public synchronized ObservationRequest moveObservation(ObservationRequest req, long startTime) {
		cancel(req.getId());
		long previousPeriod = req.getEndTimeMillis() - req.getStartTimeMillis();
		req.setStartTimeMillis(startTime);
		req.setEndTimeMillis(startTime + previousPeriod);

		TimeSlot slot = new TimeSlot();
		slot.setStart(req.getStartTimeMillis());
		slot.setEnd(req.getEndTimeMillis());
		slot.setFrequency(req.getCenterBandFrequency());
		if (!timetable.addFully(slot)) {
			return null;
		}
		index(Collections.singletonList(req));
		return req;
	}

	public synchronized List<ObservationRequest> findObservations(long startTimeMillis, long endTimeMillis) {
		List<ObservationRequest> sorted = new ArrayList<>(observationsById.values());
		Collections.sort(sorted, ObservationRequestComparator.INSTANCE);
		List<ObservationRequest> result = new ArrayList<>();
		for (ObservationRequest cur : sorted) {
			if (cur.getStartTimeMillis() > startTimeMillis && cur.getStartTimeMillis() < endTimeMillis) {
				result.add(cur);
			} else if (cur.getEndTimeMillis() > startTimeMillis && cur.getEndTimeMillis() < endTimeMillis) {
				result.add(cur);
			} else if (cur.getStartTimeMillis() < startTimeMillis && endTimeMillis < cur.getEndTimeMillis()) {
				result.add(cur);
			}
		}
		return result;
	}

	private ObservationRequest reserveFullSlot(List<ObservationRequest> allPasses) {
		for (ObservationRequest curObservation : allPasses) {
			TimeSlot slot = new TimeSlot();
			slot.setStart(curObservation.getStartTimeMillis());
			slot.setEnd(curObservation.getEndTimeMillis());
			slot.setFrequency(curObservation.getCenterBandFrequency());
			if (timetable.addFully(slot)) {
				timeSlotById.put(curObservation.getId(), slot);
				return curObservation;
			}
		}
		return null;
	}

	private ObservationRequest reservePartialSlot(List<ObservationRequest> allPasses) {
		for (ObservationRequest curObservation : allPasses) {
			TimeSlot slot = new TimeSlot();
			slot.setStart(curObservation.getStartTimeMillis());
			slot.setEnd(curObservation.getEndTimeMillis());
			slot.setFrequency(curObservation.getCenterBandFrequency());
			TimeSlot partial = timetable.addPartially(slot);
			if (partial != null) {
				curObservation.setStartTimeMillis(partial.getStart());
				curObservation.setEndTimeMillis(partial.getEnd());
				timeSlotById.put(curObservation.getId(), partial);
				return curObservation;
			}
		}
		return null;
	}

	private void removeFromIndex(ObservationRequest req) {
		List<ObservationRequest> curList = observationsBySatelliteId.get(req.getSatelliteId());
		if (curList == null) {
			return;
		}
		Iterator<ObservationRequest> it = curList.iterator();
		while (it.hasNext()) {
			ObservationRequest cur = it.next();
			if (cur.getId().equals(req.getId())) {
				it.remove();
				break;
			}
		}
	}

	private void index(List<ObservationRequest> req) {
		for (ObservationRequest cur : req) {
			List<ObservationRequest> curList = observationsBySatelliteId.get(cur.getSatelliteId());
			if (curList == null) {
				curList = new ArrayList<>();
				observationsBySatelliteId.put(cur.getSatelliteId(), curList);
			}
			curList.add(cur);
			observationsById.put(cur.getId(), cur);
		}

		for (List<ObservationRequest> values : observationsBySatelliteId.values()) {
			Collections.sort(values, ObservationRequestComparator.INSTANCE);
		}
	}

}
