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

import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterComparator;

public class Schedule {

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

	private final ObservationFactory factory;
	private final Timetable timetable;

	private final Map<String, ScheduledObservation> tasksById = new HashMap<>();
	private final Map<String, ObservationRequest> observationsById = new HashMap<>();
	private final Map<String, TimeSlot> timeSlotById = new HashMap<>();
	private final Map<String, List<ObservationRequest>> observationsByTransmitterId = new HashMap<>();

	public Schedule(Timetable timetable, ObservationFactory factory) {
		this.factory = factory;
		this.timetable = timetable;
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

	public synchronized List<ObservationRequest> createInitialSchedule(AntennaConfiguration antenna, List<Transmitter> allSatellites, long current) {
		Map<String, List<ObservationRequest>> passesBySatellite = new HashMap<>();
		for (Transmitter cur : allSatellites) {
			List<ObservationRequest> passes = factory.createSchedule(antenna, new Date(current), cur);
			if (passes.isEmpty()) {
				continue;
			}
			passesBySatellite.put(cur.getId(), passes);
		}

		List<ObservationRequest> result = new ArrayList<>();
		result.addAll(scheduleSatellites(findByPriority(allSatellites, Priority.HIGH), passesBySatellite, Priority.HIGH));
		List<Transmitter> normal = new ArrayList<>();
		List<Transmitter> explicitPriority = new ArrayList<>();
		for (Transmitter cur : allSatellites) {
			if (!cur.getPriority().equals(Priority.NORMAL)) {
				continue;
			}
			if (cur.getPriorityIndex() == 0) {
				normal.add(cur);
			} else {
				explicitPriority.add(cur);
			}
		}
		// sort by priority index
		Collections.sort(explicitPriority, TransmitterComparator.INSTANCE);
		result.addAll(scheduleSatellites(explicitPriority, passesBySatellite, Priority.NORMAL));
		result.addAll(scheduleSatellites(normal, passesBySatellite, Priority.NORMAL));

		index(result);
		Collections.sort(result, ObservationRequestComparator.INSTANCE);
		return result;
	}

	private static List<Transmitter> findByPriority(List<Transmitter> allSatellites, Priority priority) {
		List<Transmitter> result = new ArrayList<>();
		for (Transmitter cur : allSatellites) {
			if (cur.getPriority().equals(priority)) {
				result.add(cur);
			}
		}
		return result;
	}

	private List<ObservationRequest> scheduleSatellites(List<Transmitter> allSatellites, Map<String, List<ObservationRequest>> passesBySatellite, Priority priority) {
		List<ObservationRequest> result = new ArrayList<>();
		// fill-in full observations
		while (!Thread.currentThread().isInterrupted()) {
			boolean moreObservationsToCheck = false;
			for (Transmitter cur : allSatellites) {
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
			for (Transmitter cur : allSatellites) {
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
		LOG.info("{}: satellites {} observations {}", priority, allSatellites.size(), result.size());
		return result;
	}

	public synchronized List<ObservationRequest> getByTransmitterId(String transmitterId) {
		List<ObservationRequest> previous = observationsByTransmitterId.get(transmitterId);
		if (previous == null) {
			return Collections.emptyList();
		}
		return previous;
	}

	public void cancelByTransmitter(String transmitterId) {
		List<ObservationRequest> previous = observationsByTransmitterId.remove(transmitterId);
		if (previous == null) {
			return;
		}
		for (ObservationRequest cur : previous) {
			cancel(cur.getId());
		}
	}

	// even if this satellite will be scheduled well forward the rest of satellites
	// whole schedule will be cleared on next re-schedule
	public synchronized List<ObservationRequest> addToSchedule(AntennaConfiguration antenna, Transmitter transmitter, long current) {
		List<ObservationRequest> previous = observationsByTransmitterId.get(transmitter.getId());
		if (previous != null && !previous.isEmpty()) {
			return previous;
		}
		List<ObservationRequest> allPasses = factory.createSchedule(antenna, new Date(current), transmitter);
		List<ObservationRequest> batch = new ArrayList<>();
		for (ObservationRequest cur : allPasses) {
			TimeSlot slot = new TimeSlot();
			slot.setStart(cur.getStartTimeMillis());
			slot.setEnd(cur.getEndTimeMillis());
			slot.setFrequency(transmitter.getFrequencyBand());
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

	public synchronized ObservationRequest findFirstByTransmitterId(String id, long current) {
		List<ObservationRequest> curList = observationsByTransmitterId.get(id);
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
		List<ObservationRequest> curList = observationsByTransmitterId.get(req.getTransmitterId());
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
		if (curList.isEmpty()) {
			observationsByTransmitterId.remove(req.getTransmitterId());
		}
	}

	private void index(List<ObservationRequest> req) {
		for (ObservationRequest cur : req) {
			List<ObservationRequest> curList = observationsByTransmitterId.get(cur.getTransmitterId());
			if (curList == null) {
				curList = new ArrayList<>();
				observationsByTransmitterId.put(cur.getTransmitterId(), curList);
			}
			curList.add(cur);
			observationsById.put(cur.getId(), cur);
		}

		for (List<ObservationRequest> values : observationsByTransmitterId.values()) {
			Collections.sort(values, ObservationRequestComparator.INSTANCE);
		}
	}

}
