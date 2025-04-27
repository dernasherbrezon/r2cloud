package ru.r2cloud.model;

public enum DataFormat {

	COMPLEX_UNSIGNED_BYTE("cu8", 16, 8, "u8"), COMPLEX_SIGNED_SHORT("cs16", 32, 16, "s16"), COMPLEX_FLOAT("cf32", 64, 32, "f32"),

	// used for LoRa
	UNKNOWN("raw", 0, 0, "raw");

	private final int numberOfBits;
	private final int bitsPerSample;
	private final String extension;
	private final String satdump;

	private DataFormat(String extension, int bitsPerSample, int numberOfBits, String satdump) {
		this.extension = extension;
		this.bitsPerSample = bitsPerSample;
		this.numberOfBits = numberOfBits;
		this.satdump = satdump;
	}

	public String getSatdump() {
		return satdump;
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
