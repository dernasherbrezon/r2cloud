package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;

public class Schedule {

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

	private final ObservationFactory factory;
	private final Map<String, ScheduledObservation> scheduledObservations = new HashMap<>();

	public Schedule(ObservationFactory factory) {
		this.factory = factory;
	}

	public synchronized void add(ScheduledObservation entry) {
		if (entry == null) {
			return;
		}
		if (entry.getId() == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (entry.getEndTimeMillis() < entry.getStartTimeMillis()) {
			throw new IllegalArgumentException("end is less than start: " + entry.getEndTimeMillis() + " start: " + entry.getStartTimeMillis());
		}
		ScheduledObservation previous = scheduledObservations.put(entry.getId(), entry);
		if (previous != null && previous != entry) {
			LOG.info("cancelling previous: {}", previous.getStartTimeMillis());
			previous.cancel();
		}
	}

	public synchronized ScheduledObservation cancel(String id) {
		if (id == null) {
			return null;
		}
		ScheduledObservation previous = scheduledObservations.remove(id);
		if (previous == null) {
			return null;
		}
		LOG.info("cancelling {}: {}", id, previous.getStartTimeMillis());
		previous.cancel();
		return previous;
	}

	public synchronized ScheduledObservation get(String id) {
		if (id == null) {
			return null;
		}
		return scheduledObservations.get(id);
	}

	public synchronized ScheduledObservation getOverlap(long start, long end) {
		if (end < start) {
			throw new IllegalArgumentException("end is less than start: " + end + " start: " + start);
		}
		for (ScheduledObservation cur : scheduledObservations.values()) {
			if (cur.getStartTimeMillis() < start && start < cur.getEndTimeMillis()) {
				return cur;
			}
			if (cur.getStartTimeMillis() < end && end < cur.getEndTimeMillis()) {
				return cur;
			}
			if (start < cur.getStartTimeMillis() && cur.getEndTimeMillis() < end) {
				return cur;
			}
		}
		return null;
	}

	public List<ObservationRequest> createInitialSchedule(List<Satellite> allSatellites, long current) {
		List<ObservationRequest> requests = new ArrayList<>();
		for (Satellite cur : allSatellites) {
			if (!cur.isEnabled()) {
				continue;
			}
			ObservationRequest observation = getNextAvailableSlot(cur, current, false);
			if (observation == null) {
				continue;
			}
			requests.add(observation);
		}
		return requests;
	}

	public ObservationRequest getNextAvailableSlot(Satellite cur, long current, boolean immediately) {
		long next = current;
		while (!Thread.currentThread().isInterrupted()) {
			ObservationRequest observation = factory.create(new Date(next), cur, immediately);
			if (observation == null) {
				return null;
			}

			ScheduledObservation overlapped = getOverlap(observation.getStartTimeMillis(), observation.getEndTimeMillis());
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
}
