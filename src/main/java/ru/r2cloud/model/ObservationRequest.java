package ru.r2cloud.model;

import org.orekit.bodies.GeodeticPoint;

public class ObservationRequest {

	private String id;
	private long startTimeMillis;
	private long endTimeMillis;
	private String satelliteId;
	private FrequencySource source;
	private long satelliteFrequency;
	private long bandwidth;
	private Tle tle;
	private GeodeticPoint groundStation;

	private int inputSampleRate;
	private int outputSampleRate;
	private long actualFrequency;
	
	public GeodeticPoint getGroundStation() {
		return groundStation;
	}
	
	public void setGroundStation(GeodeticPoint groundStation) {
		this.groundStation = groundStation;
	}
	
	public Tle getTle() {
		return tle;
	}
	
	public void setTle(Tle tle) {
		this.tle = tle;
	}
	
	public long getBandwidth() {
		return bandwidth;
	}
	
	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public FrequencySource getSource() {
		return source;
	}

	public void setSource(FrequencySource source) {
		this.source = source;
	}

	public long getSatelliteFrequency() {
		return satelliteFrequency;
	}

	public void setSatelliteFrequency(long satelliteFrequency) {
		this.satelliteFrequency = satelliteFrequency;
	}
}
