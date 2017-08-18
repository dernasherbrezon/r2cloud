package ru.r2cloud.model;

public class RtlSdrStatus {

	private String vendor;
	private String chip;
	private String serialNumber;

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getChip() {
		return chip;
	}

	public void setChip(String chip) {
		this.chip = chip;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	@Override
	public String toString() {
		return "RtlSdrStatus [vendor=" + vendor + ", chip=" + chip + ", serialNumber=" + serialNumber + "]";
	}

}
