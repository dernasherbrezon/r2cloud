package ru.r2cloud;

import java.util.Comparator;

import ru.r2cloud.model.ObservationFull;

public class ObservationFullComparator implements Comparator<ObservationFull> {

	public static final ObservationFullComparator INSTANCE = new ObservationFullComparator();

	@Override
	public int compare(ObservationFull o1, ObservationFull o2) {
		return Long.compare(o1.getReq().getStartTimeMillis(), o2.getReq().getStartTimeMillis());
	}

}
