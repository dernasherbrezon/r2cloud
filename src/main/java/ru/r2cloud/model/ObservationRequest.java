package ru.r2cloud.model;

import uk.me.g4dpz.satellite.SatPos;

public class ObservationRequest {

	private String id;
	private long startTimeMillis;
	private SatPos start;
	private long endTimeMillis;
	private SatPos end;
	private String satelliteId;
	private String decoder;
	private long satelliteFrequency;
	private uk.me.g4dpz.satellite.Satellite origin;

	private int inputSampleRate;
	private int outputSampleRate;
	private long actualFrequency;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public uk.me.g4dpz.satellite.Satellite getOrigin() {
		return origin;
	}

	public void setOrigin(uk.me.g4dpz.satellite.Satellite origin) {
		this.origin = origin;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public long getEndTimeMillis() {
		return endTimeMillis;
	}

	public void setEndTimeMillis(long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
	}

	public SatPos getStart() {
		return start;
	}

	public void setStart(SatPos start) {
		this.start = start;
	}

	public SatPos getEnd() {
		return end;
	}

	public void setEnd(SatPos end) {
		this.end = end;
	}

	public int getInputSampleRate() {
		return inputSampleRate;
	}

	public void setInputSampleRate(int inputSampleRate) {
		this.inputSampleRate = inputSampleRate;
	}

	public int getOutputSampleRate() {
		return outputSampleRate;
	}

	public void setOutputSampleRate(int outputSampleRate) {
		this.outputSampleRate = outputSampleRate;
	}

	public long getActualFrequency() {
		return actualFrequency;
	}

	public void setActualFrequency(long actualFrequency) {
		this.actualFrequency = actualFrequency;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public String getDecoder() {
		return decoder;
	}

	public void setDecoder(String decoder) {
		this.decoder = decoder;
	}

	public long getSatelliteFrequency() {
		return satelliteFrequency;
	}

	public void setSatelliteFrequency(long satelliteFrequency) {
		this.satelliteFrequency = satelliteFrequency;
	}
}
