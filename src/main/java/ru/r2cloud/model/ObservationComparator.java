package ru.r2cloud.model;

import java.util.Comparator;

public class ObservationComparator implements Comparator<Observation> {

	public static final ObservationComparator INSTANCE = new ObservationComparator();

	@Override
	public int compare(Observation o1, Observation o2) {
		return Long.compare(o2.getStartTimeMillis(), o1.getStartTimeMillis());
	}
}
