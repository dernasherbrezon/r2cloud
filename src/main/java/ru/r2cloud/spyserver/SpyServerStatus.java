package ru.r2cloud.spyserver;

import ru.r2cloud.model.DeviceConnectionStatus;

public class SpyServerStatus {

	private DeviceConnectionStatus status;
	private String failureMessage;
	private long minFrequency;
	private long maxFrequency;

	public DeviceConnectionStatus getStatus() {
		return status;
	}

	public void setStatus(DeviceConnectionStatus status) {
		this.status = status;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public long getMinFrequency() {
		return minFrequency;
	}

	public void setMinFrequency(long minFrequency) {
		this.minFrequency = minFrequency;
	}

	public long getMaxFrequency() {
		return maxFrequency;
	}

	public void setMaxFrequency(long maxFrequency) {
		this.maxFrequency = maxFrequency;
	}

}
