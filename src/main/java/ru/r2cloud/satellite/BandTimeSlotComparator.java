package ru.r2cloud.satellite;

import java.util.Comparator;

public class BandTimeSlotComparator implements Comparator<BandTimeSlot> {

	public static final BandTimeSlotComparator INSTANCE = new BandTimeSlotComparator();

	@Override
	public int compare(BandTimeSlot o1, BandTimeSlot o2) {
		return Long.compare(o1.getStart(), o2.getStart());
	}

}
