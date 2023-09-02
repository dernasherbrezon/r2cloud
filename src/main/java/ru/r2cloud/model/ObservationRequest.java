package ru.r2cloud.model;

import org.orekit.bodies.GeodeticPoint;

public class ObservationRequest {

	private String id;
	private long startTimeMillis;
	private long endTimeMillis;
	private String satelliteId;
	private String transmitterId;
	private Tle tle;
	private GeodeticPoint groundStation;

	private SdrType sdrType;
	private int sampleRate;
	// FIXME rename to just frequency
	private long actualFrequency;
	private double gain;
	private boolean biast;
	private long centerBandFrequency;
	private int rtlDeviceId;
	private int ppm;

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

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public String getTransmitterId() {
		return transmitterId;
	}

	public void setTransmitterId(String transmitterId) {
		this.transmitterId = transmitterId;
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
	}

	public GeodeticPoint getGroundStation() {
		return groundStation;
	}

	public void setGroundStation(GeodeticPoint groundStation) {
		this.groundStation = groundStation;
	}

	public SdrType getSdrType() {
		return sdrType;
	}

	public void setSdrType(SdrType sdrType) {
		this.sdrType = sdrType;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public long getActualFrequency() {
		return actualFrequency;
	}

	public void setActualFrequency(long actualFrequency) {
		this.actualFrequency = actualFrequency;
	}

	public double getGain() {
		return gain;
	}

	public void setGain(double gain) {
		this.gain = gain;
	}

	public boolean isBiast() {
		return biast;
	}

	public void setBiast(boolean biast) {
		this.biast = biast;
	}

	public long getCenterBandFrequency() {
		return centerBandFrequency;
	}

	public void setCenterBandFrequency(long centerBandFrequency) {
		this.centerBandFrequency = centerBandFrequency;
	}

	public int getRtlDeviceId() {
		return rtlDeviceId;
	}

	public void setRtlDeviceId(int rtlDeviceId) {
		this.rtlDeviceId = rtlDeviceId;
	}

	public int getPpm() {
		return ppm;
	}

	public void setPpm(int ppm) {
		this.ppm = ppm;
	}

}
