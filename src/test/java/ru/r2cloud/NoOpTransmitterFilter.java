package ru.r2cloud;

import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.TransmitterFilter;

public class NoOpTransmitterFilter implements TransmitterFilter {
	@Override
	public boolean accept(Transmitter transmitter) {
		return true;
	}
}
