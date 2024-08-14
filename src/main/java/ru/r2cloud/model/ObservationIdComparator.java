package ru.r2cloud.model;

import java.util.Comparator;

public class ObservationIdComparator implements Comparator<String> {

	public static final ObservationIdComparator INSTANCE = new ObservationIdComparator();

	@Override
	public int compare(String o1, String o2) {
		return o2.compareTo(o1);
	}
}
