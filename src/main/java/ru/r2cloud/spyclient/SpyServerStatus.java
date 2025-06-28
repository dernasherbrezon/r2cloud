package ru.r2cloud.spyclient;

import java.util.List;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConnectionStatus;

public class SpyServerStatus {

	private DeviceConnectionStatus status;
	private String failureMessage;
	private String deviceSerial;
	private long minFrequency;
	private long maxFrequency;
	private DataFormat format;
	private List<Long> supportedSampleRates;
	
	public String getDeviceSerial() {
		return deviceSerial;
	}
	
	public void setDeviceSerial(String deviceSerial) {
		this.deviceSerial = deviceSerial;
	}
	
	public List<Long> getSupportedSampleRates() {
		return supportedSampleRates;
	}
	
	public void setSupportedSampleRates(List<Long> supportedSampleRates) {
		this.supportedSampleRates = supportedSampleRates;
	}
	
	public DataFormat getFormat() {
		return format;
	}
	
	public void setFormat(DataFormat format) {
		this.format = format;
	}

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
