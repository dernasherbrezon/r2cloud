package ru.r2cloud.model;

import java.io.IOException;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.fec.ccsds.UncorrectableException;

public class LoraBeacon extends Beacon {

	@Override
	public void readBeacon(byte[] data) throws IOException, UncorrectableException {
		// do nothing
	}

}
