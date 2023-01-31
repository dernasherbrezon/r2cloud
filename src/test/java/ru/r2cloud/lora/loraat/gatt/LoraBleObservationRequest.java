package ru.r2cloud.lora.loraat.gatt;

public class LoraBleObservationRequest {

	private long startTimeMillis;
	private long endTimeMillis;
	private long currentTime;
	private float frequency;
	private float loraBandwidth;
	private int loraSpreadFactor;
	private int loraCodingRate;
	private int loraSyncword;
	private int power;
	private int loraPreambleLength;
	private int gain;
	private int loraLdro;
	private int loraCrc;
	private int loraExplicitHeader;
	private int beaconSizeBytes;

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

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public float getFrequency() {
		return frequency;
	}

	public void setFrequency(float frequency) {
		this.frequency = frequency;
	}

	public float getLoraBandwidth() {
		return loraBandwidth;
	}

	public void setLoraBandwidth(float loraBandwidth) {
		this.loraBandwidth = loraBandwidth;
	}

	public int getLoraSpreadFactor() {
		return loraSpreadFactor;
	}

	public void setLoraSpreadFactor(int loraSpreadFactor) {
		this.loraSpreadFactor = loraSpreadFactor;
	}

	public int getLoraCodingRate() {
		return loraCodingRate;
	}

	public void setLoraCodingRate(int loraCodingRate) {
		this.loraCodingRate = loraCodingRate;
	}

	public int getLoraSyncword() {
		return loraSyncword;
	}

	public void setLoraSyncword(int loraSyncword) {
		this.loraSyncword = loraSyncword;
	}

	public int getPower() {
		return power;
	}

	public void setPower(int power) {
		this.power = power;
	}

	public int getLoraPreambleLength() {
		return loraPreambleLength;
	}

	public void setLoraPreambleLength(int loraPreambleLength) {
		this.loraPreambleLength = loraPreambleLength;
	}

	public int getGain() {
		return gain;
	}

	public void setGain(int gain) {
		this.gain = gain;
	}

	public int getLoraLdro() {
		return loraLdro;
	}

	public void setLoraLdro(int loraLdro) {
		this.loraLdro = loraLdro;
	}

	public int getLoraCrc() {
		return loraCrc;
	}

	public void setLoraCrc(int loraCrc) {
		this.loraCrc = loraCrc;
	}

	public int getLoraExplicitHeader() {
		return loraExplicitHeader;
	}

	public void setLoraExplicitHeader(int loraExplicitHeader) {
		this.loraExplicitHeader = loraExplicitHeader;
	}

	public int getBeaconSizeBytes() {
		return beaconSizeBytes;
	}

	public void setBeaconSizeBytes(int beaconSizeBytes) {
		this.beaconSizeBytes = beaconSizeBytes;
	}

}
