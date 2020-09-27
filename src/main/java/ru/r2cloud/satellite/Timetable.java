package ru.r2cloud.satellite;

import java.util.LinkedList;
import java.util.List;

public class Timetable {

	private List<TimeSlot> slots = new LinkedList<>();

	private final long partialToleranceMillis;

	public Timetable(long partialToleranceMillis) {
		this.partialToleranceMillis = partialToleranceMillis;
	}

	public boolean addFully(TimeSlot slot) {
		// add slot to the list
		// keep slots sorted by time
		if (slots.isEmpty()) {
			slots.add(slot);
			return true;
		}
		TimeSlot first = slots.get(0);
		if (slot.getEnd() <= first.getStart()) {
			slots.add(0, slot);
			return true;
		}
		for (int i = 0; i < slots.size(); i++) {
			TimeSlot cur = slots.get(i);
			if (i + 1 < slots.size()) {
				TimeSlot next = slots.get(i + 1);
				if (cur.getEnd() <= slot.getStart() && slot.getEnd() <= next.getStart()) {
					slots.add(i + 1, slot);
					return true;
				}
			} else {
				if (cur.getEnd() <= slot.getStart()) {
					slots.add(slot);
					return true;
				}
			}
		}
		return false;
	}

	public TimeSlot addPatially(TimeSlot slot) {
		if (slots.isEmpty()) {
			slots.add(slot);
			return slot;
		}
		long toleranceEnd = slot.getEnd() - partialToleranceMillis;
		long toleranceStart = slot.getStart() + partialToleranceMillis;
		TimeSlot first = slots.get(0);
		if (toleranceEnd <= first.getStart()) {
			TimeSlot partial = new TimeSlot();
			partial.setStart(slot.getStart());
			partial.setEnd(getEnd(slot, first));
			slots.add(0, partial);
			return partial;
		}
		for (int i = 0; i < slots.size(); i++) {
			TimeSlot cur = slots.get(i);
			if (i + 1 < slots.size()) {
				TimeSlot next = slots.get(i + 1);
				boolean fitStart = cur.getEnd() <= slot.getStart();
				boolean fitEnd = slot.getEnd() <= next.getStart();
				if (fitStart && fitEnd) {
					slots.add(i + 1, slot);
					return slot;
				}
				boolean fitToleranceStart = cur.getEnd() <= toleranceStart;
				boolean fitToleranceEnd = toleranceEnd <= next.getStart();
				if (fitStart && fitToleranceEnd) {
					TimeSlot partial = new TimeSlot();
					partial.setStart(slot.getStart());
					partial.setEnd(getEnd(slot, next));
					slots.add(i + 1, partial);
					return partial;
				}
				if (fitToleranceStart && fitEnd) {
					TimeSlot partial = new TimeSlot();
					partial.setStart(getStart(slot, cur));
					partial.setEnd(slot.getEnd());
					slots.add(i + 1, partial);
					return partial;
				}
				if (fitToleranceStart && fitToleranceEnd && toleranceEnd - toleranceStart >= partialToleranceMillis) {
					TimeSlot partial = new TimeSlot();
					partial.setStart(getStart(slot, cur));
					partial.setEnd(getEnd(slot, next));
					slots.add(i + 1, partial);
					return partial;
				}
			} else {
				if (cur.getEnd() <= toleranceStart) {
					TimeSlot partial = new TimeSlot();
					partial.setStart(getStart(slot, cur));
					partial.setEnd(slot.getEnd());
					slots.add(partial);
					return partial;
				}
			}
		}
		return null;
	}

	private static long getStart(TimeSlot slot, TimeSlot existing) {
		if (slot.getStart() > existing.getEnd()) {
			return slot.getStart();
		}
		return existing.getEnd();
	}

	private static long getEnd(TimeSlot slot, TimeSlot existing) {
		if (slot.getEnd() < existing.getStart()) {
			return slot.getEnd();
		}
		return existing.getStart();
	}

	public void clear() {
		slots.clear();
	}
}
