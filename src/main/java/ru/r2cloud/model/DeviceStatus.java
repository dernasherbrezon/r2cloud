package ru.r2cloud.model;

public class DeviceStatus {

	private DeviceType type;
	private DeviceConfiguration config;
	private DeviceConnectionStatus status;
	private String failureMessage;
	private String model;
	private RotatorStatus rotatorStatus;

	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}
	
	public DeviceType getType() {
		return type;
	}

	public void setType(DeviceType type) {
		this.type = type;
	}

	public DeviceConfiguration getConfig() {
		return config;
	}

	public void setConfig(DeviceConfiguration config) {
		this.config = config;
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

	public RotatorStatus getRotatorStatus() {
		return rotatorStatus;
	}

	public void setRotatorStatus(RotatorStatus rotatorStatus) {
		this.rotatorStatus = rotatorStatus;
	}

}
