package ru.r2cloud.model;

import java.util.Comparator;

public class SatelliteComparator implements Comparator<Satellite> {

	public static final SatelliteComparator ID_COMPARATOR = new SatelliteComparator(false);
	public static final SatelliteComparator FREQ_BANDWIDTH_COMPARATOR = new SatelliteComparator(true);

	private final boolean compareByFrequency;

	public SatelliteComparator(boolean compareByFrequency) {
		this.compareByFrequency = compareByFrequency;
	}

	@Override
	public int compare(Satellite o1, Satellite o2) {
		if (compareByFrequency) {
			long o1lowerFrequency = o1.getFrequency() - o1.getInputSampleRate() / 2;
			long o2lowerFrequency = o2.getFrequency() - o2.getInputSampleRate() / 2;
			return Long.compare(o1lowerFrequency, o2lowerFrequency);
		}
		return o1.getId().compareTo(o2.getId());
	}

}
