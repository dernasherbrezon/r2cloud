package ru.r2cloud.model;

public class SdrStatus {

	private DeviceConnectionStatus status;
	private String failureMessage;
	private String model;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
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

}
