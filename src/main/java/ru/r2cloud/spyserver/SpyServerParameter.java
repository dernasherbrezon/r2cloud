package ru.r2cloud.spyserver;

public enum SpyServerParameter {

	SPYSERVER_SETTING_STREAMING_MODE(0, false), SPYSERVER_SETTING_STREAMING_ENABLED(1, true), SPYSERVER_SETTING_GAIN(2, true), SPYSERVER_SETTING_IQ_FORMAT(100, true), SPYSERVER_SETTING_IQ_FREQUENCY(101, true), SPYSERVER_SETTING_IQ_DECIMATION(102, true);

	// FFT parameters are not supported

	private final int code;
	private final boolean async;

	private SpyServerParameter(int code, boolean async) {
		this.code = code;
		this.async = async;
	}
	
	public boolean isAsync() {
		return async;
	}

	public int getCode() {
		return code;
	}

}
