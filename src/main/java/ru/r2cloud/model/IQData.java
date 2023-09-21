package ru.r2cloud.model;

import java.io.File;

public class IQData {

	private DataFormat dataFormat;
	private File dataFile;
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

	public File getDataFile() {
		return dataFile;
	}
	
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}

}
