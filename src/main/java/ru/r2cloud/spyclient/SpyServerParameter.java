package ru.r2cloud.spyclient;

public enum SpyServerParameter {

	SPYSERVER_SETTING_STREAMING_MODE(0), SPYSERVER_SETTING_STREAMING_ENABLED(1), SPYSERVER_SETTING_GAIN(2), SPYSERVER_SETTING_IQ_FORMAT(100), SPYSERVER_SETTING_IQ_FREQUENCY(101), SPYSERVER_SETTING_IQ_DECIMATION(102), SPYSERVER_SETTING_IQ_DIGITAL_GAIN(103);

	// FFT parameters are not supported

	private final int code;

	private SpyServerParameter(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
