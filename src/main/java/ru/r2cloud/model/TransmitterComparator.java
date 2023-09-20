package ru.r2cloud.model;

import java.util.Comparator;

public class TransmitterComparator implements Comparator<Transmitter> {
	
	public static final TransmitterComparator INSTANCE = new TransmitterComparator();
	
	@Override
	public int compare(Transmitter o1, Transmitter o2) {
		return Integer.compare(o2.getPriorityIndex(), o1.getPriorityIndex());
	}

}
