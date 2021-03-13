package ru.r2cloud.satellite;

import java.util.LinkedList;
import java.util.List;

public class BandTimeSlot {

	private List<TimeSlot> slots = new LinkedList<>();
	private long frequency;
	private long start;
	private long end;

	public BandTimeSlot() {
		// do nothing
	}

	public BandTimeSlot(TimeSlot slot) {
		start = slot.getStart();
		end = slot.getEnd();
		frequency = slot.getFrequency();
		slots.add(slot);
	}

	public void addSlot(TimeSlot slot) {
		slots.add(slot);
	}

	public List<TimeSlot> getSlots() {
		return slots;
	}

	public void setSlots(List<TimeSlot> slots) {
		this.slots = slots;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

}
