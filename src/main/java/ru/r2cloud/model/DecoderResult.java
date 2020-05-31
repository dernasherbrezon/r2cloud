package ru.r2cloud.model;

import java.io.File;

public class DecoderResult {

	private File rawPath;

	private String gain;
	private String channelA;
	private String channelB;
	private Long numberOfDecodedPackets = 0L;

	private File imagePath;
	private File dataPath;

	public File getRawPath() {
		return rawPath;
	}
	
	public void setRawPath(File rawPath) {
		this.rawPath = rawPath;
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

	public File getImagePath() {
		return imagePath;
	}

	public void setImagePath(File imagePath) {
		this.imagePath = imagePath;
	}

	public File getDataPath() {
		return dataPath;
	}

	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}

}
