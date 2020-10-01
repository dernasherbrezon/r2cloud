package ru.r2cloud.satellite;

import java.util.Comparator;

import ru.r2cloud.model.ObservationRequest;

public class ObservationRequestComparator implements Comparator<ObservationRequest> {
	
	public static final ObservationRequestComparator INSTANCE = new ObservationRequestComparator();

	@Override
	public int compare(ObservationRequest o1, ObservationRequest o2) {
		return Long.compare(o1.getStartTimeMillis(), o2.getStartTimeMillis());
	}
}
