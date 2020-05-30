package ru.r2cloud.model;

import java.io.File;

public class DecoderResult {

	private File wavPath;
	private File iqPath;

	private String gain;
	private String channelA;
	private String channelB;
	private Long numberOfDecodedPackets = 0L;

	private File aPath;
	private File dataPath;

	public File getIqPath() {
		return iqPath;
	}

	public void setIqPath(File iqPath) {
		this.iqPath = iqPath;
	}

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

	public File getaPath() {
		return aPath;
	}

	public void setaPath(File aPath) {
		this.aPath = aPath;
	}

	public File getDataPath() {
		return dataPath;
	}

	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}

}
