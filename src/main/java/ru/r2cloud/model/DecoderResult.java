package ru.r2cloud.model;

import java.io.File;
import java.util.List;

public class DecoderResult {

	private File iq;

	private String channelA;
	private String channelB;
	private int numberOfDecodedPackets;
	private Long totalSize = 0L;

	private File image;
	private File data;
	private List<Instrument> instruments;
	
	public List<Instrument> getInstruments() {
		return instruments;
	}
	
	public void setInstruments(List<Instrument> instruments) {
		this.instruments = instruments;
	}
	
	public Long getTotalSize() {
		return totalSize;
	}
	
	public void setTotalSize(Long totalSize) {
		this.totalSize = totalSize;
	}

	public File getIq() {
		return iq;
	}
	
	public void setIq(File iq) {
		this.iq = iq;
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

	public File getImage() {
		return image;
	}

	public void setImage(File image) {
		this.image = image;
	}

	public File getData() {
		return data;
	}

	public void setData(File data) {
		this.data = data;
	}

}
