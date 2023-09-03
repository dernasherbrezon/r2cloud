package ru.r2cloud.model;

import java.io.File;

public class IQData {

	private DataFormat dataFormat;
	private File dataFile;
	private long actualStart;
	private long actualEnd;
	private int inputSampleRate;
	private int outputSampleRate;
	
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
