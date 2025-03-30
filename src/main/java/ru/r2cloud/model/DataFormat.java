package ru.r2cloud.model;

public enum DataFormat {

	COMPLEX_UNSIGNED_BYTE("cu8", 16, 8, "u8"), COMPLEX_SIGNED_SHORT("cs16", 32, 16, "s16"), COMPLEX_FLOAT("cf32", 64, 32, "f32"),

	// used for LoRa
	UNKNOWN("raw", 0, 0, "raw");

	private final int numberOfBits;
	private final int bitsPerSample;
	private final String extension;
	private final String satdumpFormat;

	private DataFormat(String extension, int bitsPerSample, int numberOfBits, String satdumpFormat) {
		this.extension = extension;
		this.bitsPerSample = bitsPerSample;
		this.numberOfBits = numberOfBits;
		this.satdumpFormat = satdumpFormat;
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

	public String getSatdumpFormat() {
		return satdumpFormat;
	}
}
