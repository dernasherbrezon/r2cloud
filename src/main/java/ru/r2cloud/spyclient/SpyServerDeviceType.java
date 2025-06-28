package ru.r2cloud.spyclient;

public enum SpyServerDeviceType {

	AIRSPY_ONE(1), AIRSPY_HF(2), RTLSDR(3), INVALID(0);

	private final long code;

	private SpyServerDeviceType(long code) {
		this.code = code;
	}
	
	public long getCode() {
		return code;
	}

	public static SpyServerDeviceType valueOfCode(long code) {
		for (SpyServerDeviceType cur : values()) {
			if (cur.code == code) {
				return cur;
			}
		}
		return INVALID;
	}

}
