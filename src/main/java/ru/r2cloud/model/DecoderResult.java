package ru.r2cloud.model;

import java.io.File;

public class DecoderResult {

	private File rawPath;

	private String channelA;
	private String channelB;
	private int numberOfDecodedPackets;
	private Long totalSize = 0L;

	private File imagePath;
	private File dataPath;
	
	public Long getTotalSize() {
		return totalSize;
	}
	
	public void setTotalSize(Long totalSize) {
		this.totalSize = totalSize;
	}

	public File getRawPath() {
		return rawPath;
	}
	
	public void setRawPath(File rawPath) {
		this.rawPath = rawPath;
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

	public int getNumberOfDecodedPackets() {
		return numberOfDecodedPackets;
	}

	public void setNumberOfDecodedPackets(int numberOfDecodedPackets) {
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
