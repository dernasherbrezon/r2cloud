package ru.r2cloud.model;

import java.io.File;

public class IQData {

	private File wavFile;
	private File iqFile;
	private long actualStart;
	private long actualEnd;

	public File getIqFile() {
		return iqFile;
	}

	public void setIqFile(File iqFile) {
		this.iqFile = iqFile;
	}

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

	public boolean hasDataFile() {
		if (wavFile != null && wavFile.exists()) {
			return true;
		}
		if (iqFile != null && iqFile.exists()) {
			return true;
		}
		return false;
	}

	public File getDataFile() {
		if (wavFile != null) {
			return wavFile;
		}
		return iqFile;
	}

}
