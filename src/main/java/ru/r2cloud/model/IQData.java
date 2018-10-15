package ru.r2cloud.model;

import java.io.File;

public class IQData {

	private File wavFile;
	private long actualStart;
	private long actualEnd;

	public File getWavFile() {
		return wavFile;
	}

	public void setWavFile(File wavFile) {
		this.wavFile = wavFile;
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

}
