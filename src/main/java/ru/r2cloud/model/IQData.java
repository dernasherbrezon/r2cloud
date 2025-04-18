package ru.r2cloud.model;

import java.io.File;

public class IQData {

	private DataFormat dataFormat;
	private File iq;
	private long actualStart;
	private long actualEnd;
	private long sampleRate;
	
	public long getSampleRate() {
		return sampleRate;
	}
	
	public void setSampleRate(long sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	public DataFormat getDataFormat() {
		return dataFormat;
	}
	
	public void setDataFormat(DataFormat dataFormat) {
		this.dataFormat = dataFormat;
	}

	public long getActualStart() {
		return actualStart;
	}

	public void setActualStart(long actualStart) {
		this.actualStart = actualStart;
	}

	public long getActualEnd() {
		return actualEnd;
	}

	public void setActualEnd(long actualEnd) {
		this.actualEnd = actualEnd;
	}

	public File getIq() {
		return iq;
	}
	
	public void setIq(File iq) {
		this.iq = iq;
	}

}
