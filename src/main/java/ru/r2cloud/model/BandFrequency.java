package ru.r2cloud.model;

public class BandFrequency {

	private Transmitter transmitter;
	private long center;
	private long lower;
	private long upper;

	public Transmitter getTransmitter() {
		return transmitter;
	}

	public void setTransmitter(Transmitter transmitter) {
		this.transmitter = transmitter;
	}

	public long getCenter() {
		return center;
	}

	public void setCenter(long center) {
		this.center = center;
	}

	public long getLower() {
		return lower;
	}

	public void setLower(long lower) {
		this.lower = lower;
	}

	public long getUpper() {
		return upper;
	}

	public void setUpper(long upper) {
		this.upper = upper;
	}

}
