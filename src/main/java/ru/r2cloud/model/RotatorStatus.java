package ru.r2cloud.model;

public class RotatorStatus {

	private String hostport;
	private String model;
	private DeviceConnectionStatus status;
	private String failureMessage;

	public String getHostport() {
		return hostport;
	}

	public void setHostport(String hostport) {
		this.hostport = hostport;
	}

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
