package ru.r2cloud.model;

import java.io.File;

public class LRPTResult {

	private File data;
	private File image;
	private long numberOfDecodedPackets;

	public long getNumberOfDecodedPackets() {
		return numberOfDecodedPackets;
	}

	public void setNumberOfDecodedPackets(long numberOfDecodedPackets) {
		this.numberOfDecodedPackets = numberOfDecodedPackets;
	}

	public File getData() {
		return data;
	}

	public void setData(File data) {
		this.data = data;
	}

	public File getImage() {
		return image;
	}

	public void setImage(File image) {
		this.image = image;
	}

}
