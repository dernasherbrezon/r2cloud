package ru.r2cloud.satellite;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Schedule<T extends ScheduleEntry> {

	private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

	private final Map<String, T> scheduledObservations = new HashMap<>();

	public synchronized void add(T entry) {
		if (entry == null) {
			return;
		}
		if (entry.getId() == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (entry.getEndTimeMillis() < entry.getStartTimeMillis()) {
			throw new IllegalArgumentException("end is less than start: " + entry.getEndTimeMillis() + " start: " + entry.getStartTimeMillis());
		}
		T previous = scheduledObservations.put(entry.getId(), entry);
		if (previous != null && previous != entry) {
			LOG.info("cancelling previous: {}", previous.getStartTimeMillis());
			previous.cancel();
		}
	}

	public synchronized T cancel(String id) {
		if (id == null) {
			return null;
		}
		T previous = scheduledObservations.remove(id);
		if (previous == null) {
			return null;
		}
		LOG.info("cancelling {}: {}", id, previous.getStartTimeMillis());
		previous.cancel();
		return previous;
	}

	public synchronized T get(String id) {
		if (id == null) {
			return null;
		}
		return scheduledObservations.get(id);
	}

	public synchronized T getOverlap(long start, long end) {
		if (end < start) {
			throw new IllegalArgumentException("end is less than start: " + end + " start: " + start);
		}
		for (T cur : scheduledObservations.values()) {
			if (cur.getStartTimeMillis() < start && start < cur.getEndTimeMillis()) {
				return cur;
			}
			if (cur.getStartTimeMillis() < end && end < cur.getEndTimeMillis()) {
				return cur;
			}
		}
		return null;
	}
}
