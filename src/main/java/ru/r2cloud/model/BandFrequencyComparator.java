package ru.r2cloud.model;

import java.util.Comparator;

public class BandFrequencyComparator implements Comparator<BandFrequency> {

	public static final BandFrequencyComparator INSTANCE = new BandFrequencyComparator();

	@Override
	public int compare(BandFrequency o1, BandFrequency o2) {
		return Long.compare(o1.getCenter(), o2.getCenter());
	}

}
