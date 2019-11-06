package ru.r2cloud.model;

import uk.me.g4dpz.satellite.GroundStationPosition;

public class ObservationRequest {

	private String id;
	private long startTimeMillis;
	private double startLatitude;
	private long endTimeMillis;
	private double endLatitude;
	private String satelliteId;
	private FrequencySource source;
	private long satelliteFrequency;
	private long bandwidth;
	private Tle tle;
	private GroundStationPosition groundStation;

	private int inputSampleRate;
	private int outputSampleRate;
	private long actualFrequency;
	
	public GroundStationPosition getGroundStation() {
		return groundStation;
	}
	
	public void setGroundStation(GroundStationPosition groundStation) {
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

	public double getStartLatitude() {
		return startLatitude;
	}

	public void setStartLatitude(double startLatitude) {
		this.startLatitude = startLatitude;
	}

	public double getEndLatitude() {
		return endLatitude;
	}

	public void setEndLatitude(double endLatitude) {
		this.endLatitude = endLatitude;
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
