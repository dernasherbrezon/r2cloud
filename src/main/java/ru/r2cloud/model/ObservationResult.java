package ru.r2cloud.model;

import java.io.File;

public class ObservationResult {

	private File wavPath;

	private String gain;
	private String channelA;
	private String channelB;
	private Long numberOfDecodedPackets;

	private String aURL;
	private File aPath;

	private String spectogramURL;
	private File spectogramPath;

	private String dataURL;
	private File dataPath;

	public File getWavPath() {
		return wavPath;
	}

	public void setWavPath(File wavPath) {
		this.wavPath = wavPath;
	}

	public String getGain() {
		return gain;
	}

	public void setGain(String gain) {
		this.gain = gain;
	}

	public String getChannelA() {
		return channelA;
	}

	public void setChannelA(String channelA) {
		this.channelA = channelA;
	}

	public String getChannelB() {
		return channelB;
	}

	public void setChannelB(String channelB) {
		this.channelB = channelB;
	}

	public Long getNumberOfDecodedPackets() {
		return numberOfDecodedPackets;
	}

	public void setNumberOfDecodedPackets(Long numberOfDecodedPackets) {
		this.numberOfDecodedPackets = numberOfDecodedPackets;
	}

	public String getaURL() {
		return aURL;
	}

	public void setaURL(String aURL) {
		this.aURL = aURL;
	}

	public File getaPath() {
		return aPath;
	}

	public void setaPath(File aPath) {
		this.aPath = aPath;
	}

	public String getSpectogramURL() {
		return spectogramURL;
	}

	public void setSpectogramURL(String spectogramURL) {
		this.spectogramURL = spectogramURL;
	}

	public File getSpectogramPath() {
		return spectogramPath;
	}

	public void setSpectogramPath(File spectogramPath) {
		this.spectogramPath = spectogramPath;
	}

	public String getDataURL() {
		return dataURL;
	}

	public void setDataURL(String dataURL) {
		this.dataURL = dataURL;
	}

	public File getDataPath() {
		return dataPath;
	}

	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}

}
