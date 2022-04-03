package ru.r2cloud.model;

import java.util.Comparator;

public class TransmitterComparator implements Comparator<Transmitter> {

	public static final TransmitterComparator INSTANCE = new TransmitterComparator();

	@Override
	public int compare(Transmitter o1, Transmitter o2) {
		long o1lowerFrequency = o1.getFrequency() - o1.getInputSampleRate() / 2;
		long o2lowerFrequency = o2.getFrequency() - o2.getInputSampleRate() / 2;
		return Long.compare(o1lowerFrequency, o2lowerFrequency);
	}

}
