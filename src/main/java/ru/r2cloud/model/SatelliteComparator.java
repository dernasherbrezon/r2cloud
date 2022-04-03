package ru.r2cloud.model;

import java.util.Comparator;

public class SatelliteComparator implements Comparator<Satellite> {

	public static final SatelliteComparator ID_COMPARATOR = new SatelliteComparator();

	@Override
	public int compare(Satellite o1, Satellite o2) {
		return o1.getId().compareTo(o2.getId());
	}

}
