package ru.r2cloud.satellite;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OverlappedTimetable {

	private List<BandTimeSlot> bands = new LinkedList<>();

	private final long partialToleranceMillis;

	public OverlappedTimetable(long partialToleranceMillis) {
		this.partialToleranceMillis = partialToleranceMillis;
	}

	public boolean addFully(TimeSlot slot) {
		if (bands.isEmpty()) {
			bands.add(new BandTimeSlot(slot));
			return true;
		}
		// bands are sorted
		boolean result = false;
		Integer mergedBandTimeSlot = null;
		for (int i = 0; i < bands.size(); i++) {
			BandTimeSlot cur = bands.get(i);
			BandTimeSlot previous = null;
			if (i > 0) {
				previous = bands.get(i - 1);
			}
			boolean last = i == bands.size() - 1;
			// 1. slot fully sits in the current
			if (cur.getStart() <= slot.getStart() && slot.getEnd() <= cur.getEnd()) {
				if (slot.getFrequency() == cur.getFrequency()) {
					cur.addSlot(slot);
					result = true;
				}
				break;
			}
			// 2. slot is between the previous and current
			if (previous != null && previous.getEnd() < slot.getStart() && slot.getEnd() < cur.getStart()) {
				bands.add(new BandTimeSlot(slot));
				result = true;
				break;
			}
			// 3. slot intersects with both previous and current
			if (previous != null && slot.getStart() <= previous.getEnd() && cur.getStart() <= slot.getEnd()) {
				// if both bands are the same and slot is the same, then merge all together
				if (slot.getFrequency() == previous.getFrequency() && slot.getFrequency() == cur.getFrequency()) {
					previous.setEnd(cur.getEnd());
					previous.addSlot(slot);
					// copy slots from the cur
					for (TimeSlot curSlot : cur.getSlots()) {
						previous.addSlot(curSlot);
					}
					mergedBandTimeSlot = i;
					result = true;
				}
				break;
			}
			// 4. slot is before the first (current)
			if (previous == null && slot.getEnd() < cur.getStart()) {
				bands.add(new BandTimeSlot(slot));
				result = true;
				break;
			}
			// 5. slot is after the last (current)
			if (last && cur.getEnd() < slot.getStart()) {
				bands.add(new BandTimeSlot(slot));
				result = true;
				break;
			}
			// 6. slot intersects with the current
			if (slot.getStart() <= cur.getStart() && cur.getStart() <= slot.getEnd()) {
				if (cur.getFrequency() == slot.getFrequency()) {
					cur.setStart(Math.min(slot.getStart(), cur.getStart()));
					cur.setEnd(Math.max(slot.getEnd(), cur.getEnd()));
					cur.addSlot(slot);
					result = true;
				}
				break;
			}
			// 7. slot intersects with the current
			if (last && cur.getStart() <= slot.getStart() && slot.getStart() <= cur.getEnd()) {
				if (cur.getFrequency() == slot.getFrequency()) {
					cur.setStart(Math.min(slot.getStart(), cur.getStart()));
					cur.setEnd(Math.max(slot.getEnd(), cur.getEnd()));
					cur.addSlot(slot);
					result = true;
				}
				break;
			}
			// 8. slot intersects with the previous
			if (previous != null && slot.getStart() <= previous.getEnd() && slot.getEnd() < cur.getStart()) {
				if (cur.getFrequency() == slot.getFrequency()) {
					previous.setStart(Math.min(slot.getStart(), previous.getStart()));
					previous.setEnd(Math.max(slot.getEnd(), previous.getEnd()));
					previous.addSlot(slot);
					result = true;
				}
				break;
			}

		}

		if (mergedBandTimeSlot != null) {
			bands.remove(mergedBandTimeSlot.intValue());
		}
		if (result) {
			Collections.sort(bands, BandTimeSlotComparator.INSTANCE);
		}
		return result;
	}

	public TimeSlot addPartially(TimeSlot slot) {
		if (bands.isEmpty()) {
			bands.add(new BandTimeSlot(slot));
			return slot;
		}
		TimeSlot result = null;
		for (int i = 0; i < bands.size(); i++) {
			BandTimeSlot cur = bands.get(i);
			BandTimeSlot previous = null;
			if (i > 0) {
				previous = bands.get(i - 1);
			}
			boolean last = i == bands.size() - 1;
			// 1. slot intersects both previous and current
			if (previous != null && slot.getStart() <= previous.getEnd() && cur.getStart() <= slot.getEnd()) {
				boolean matchesPreviousFreq = slot.getFrequency() == previous.getFrequency() && slot.getFrequency() != cur.getFrequency();
				boolean matchesCurrentFreq = slot.getFrequency() != previous.getFrequency() && slot.getFrequency() == cur.getFrequency();
				boolean matchesNoneFreq = slot.getFrequency() != previous.getFrequency() && slot.getFrequency() != cur.getFrequency();
				if (matchesPreviousFreq && cur.getStart() - slot.getStart() >= partialToleranceMillis) {
					previous.setEnd(cur.getStart());
					result = new TimeSlot();
					result.setStart(slot.getStart());
					result.setEnd(cur.getStart());
					result.setFrequency(slot.getFrequency());
					previous.addSlot(result);
				}
				if (matchesCurrentFreq && slot.getEnd() - previous.getEnd() >= partialToleranceMillis) {
					cur.setStart(previous.getEnd());
					result = new TimeSlot();
					result.setStart(previous.getEnd());
					result.setEnd(slot.getEnd());
					result.setFrequency(slot.getFrequency());
					cur.addSlot(result);
				}
				if (matchesNoneFreq && cur.getStart() - previous.getEnd() >= partialToleranceMillis) {
					result = new TimeSlot();
					result.setStart(previous.getEnd());
					result.setEnd(cur.getStart());
					result.setFrequency(slot.getFrequency());
					bands.add(new BandTimeSlot(result));
				}
				break;
			}
			if (previous == null && cur.getStart() <= slot.getEnd() && slot.getEnd() <= cur.getEnd()) {
				if (slot.getFrequency() != cur.getFrequency() && cur.getStart() - slot.getStart() >= partialToleranceMillis) {
					result = new TimeSlot();
					result.setStart(slot.getStart());
					result.setEnd(cur.getStart());
					result.setFrequency(slot.getFrequency());
					bands.add(new BandTimeSlot(result));
				}
				break;
			}
			if (last && cur.getStart() <= slot.getStart() && slot.getStart() <= cur.getEnd()) {
				if (cur.getFrequency() != slot.getFrequency() && slot.getEnd() - cur.getEnd() >= partialToleranceMillis) {
					result = new TimeSlot();
					result.setStart(cur.getEnd());
					result.setEnd(slot.getEnd());
					result.setFrequency(slot.getFrequency());
					bands.add(new BandTimeSlot(result));
				}
				break;
			}
			if (previous != null && slot.getStart() <= previous.getEnd() && slot.getEnd() < cur.getStart()) {
				if (previous.getFrequency() != slot.getFrequency() && slot.getEnd() - previous.getEnd() >= partialToleranceMillis) {
					result = new TimeSlot();
					result.setStart(previous.getEnd());
					result.setEnd(slot.getEnd());
					result.setFrequency(slot.getFrequency());
					bands.add(new BandTimeSlot(result));
				}
				break;
			}

		}
		if (result != null) {
			Collections.sort(bands, BandTimeSlotComparator.INSTANCE);
		}
		return result;
	}

	public void clear() {
		bands.clear();
	}

	public boolean remove(TimeSlot slot) {
		if (slot == null) {
			return false;
		}
		Iterator<BandTimeSlot> it = bands.iterator();
		while (it.hasNext()) {
			BandTimeSlot cur = it.next();
			if (cur.getSlots().remove(slot)) {
				if (cur.getSlots().isEmpty()) {
					it.remove();
				}
				return true;
			}
		}
		return false;
	}
}
