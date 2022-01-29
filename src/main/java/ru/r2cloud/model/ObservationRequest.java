package ru.r2cloud.model;

import java.util.List;

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
	private List<Integer> baudRates;

	private int inputSampleRate;
	private int outputSampleRate;
	private long actualFrequency;
	private double gain;
	private boolean biast;
	private SdrType sdrType;
	private long centerBandFrequency;
	private int rtlDeviceId;
	private int ppm;
	private SdrServerConfiguration sdrServerConfiguration;
	
	public List<Integer> getBaudRates() {
		return baudRates;
	}
	
	public void setBaudRates(List<Integer> baudRates) {
		this.baudRates = baudRates;
	}
	
	public SdrServerConfiguration getSdrServerConfiguration() {
		return sdrServerConfiguration;
	}
	
	public void setSdrServerConfiguration(SdrServerConfiguration sdrServerConfiguration) {
		this.sdrServerConfiguration = sdrServerConfiguration;
	}
	
	public int getPpm() {
		return ppm;
	}
	
	public void setPpm(int ppm) {
		this.ppm = ppm;
	}
	
	public int getRtlDeviceId() {
		return rtlDeviceId;
	}
	
	public void setRtlDeviceId(int rtlDeviceId) {
		this.rtlDeviceId = rtlDeviceId;
	}
	
	public long getCenterBandFrequency() {
		return centerBandFrequency;
	}
	
	public void setCenterBandFrequency(long centerBandFrequency) {
		this.centerBandFrequency = centerBandFrequency;
	}
	
	public SdrType getSdrType() {
		return sdrType;
	}
	
	public void setSdrType(SdrType sdrType) {
		this.sdrType = sdrType;
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
