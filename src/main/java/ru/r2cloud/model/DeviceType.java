package ru.r2cloud.model;

public enum DeviceType {

	RTLSDR("rtlsdr"), PLUTOSDR("plutosdr"), LORAAT(null), LORAATBLE(null), LORAATBLEC(null), LORAATWIFI(null), SDRSERVER(null), SPYSERVER("spyserver"), AIRSPY("airspy");

	private final String satdumpCode;

	private DeviceType(String satdumpCode) {
		this.satdumpCode = satdumpCode;
	}

	// FIXME remove. not used anymore
	public String getSatdumpCode() {
		return satdumpCode;
	}

}
