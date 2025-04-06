package ru.r2cloud.model;

public enum DataFormat {

	COMPLEX_UNSIGNED_BYTE("cu8", 16, 8), COMPLEX_SIGNED_SHORT("cs16", 32, 16), COMPLEX_FLOAT("cf32", 64, 32),

	// ziq files contain header with the bits format
	ZIQ("ziq", 0, 0),

	// used for LoRa
	UNKNOWN("raw", 0, 0);

	private final int numberOfBits;
	private final int bitsPerSample;
	private final String extension;

	private DataFormat(String extension, int bitsPerSample, int numberOfBits) {
		this.extension = extension;
		this.bitsPerSample = bitsPerSample;
		this.numberOfBits = numberOfBits;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public String getExtension() {
		return extension;
	}

	public int getNumberOfBits() {
		return numberOfBits;
	}

}
