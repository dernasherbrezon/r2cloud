package ru.r2cloud;

import java.util.Comparator;

import ru.r2cloud.model.Observation;

public class ObservationFullComparator implements Comparator<Observation> {

	public static final ObservationFullComparator INSTANCE = new ObservationFullComparator();

	@Override
	public int compare(Observation o1, Observation o2) {
		return Long.compare(o2.getStartTimeMillis(), o1.getStartTimeMillis());
	}

}
