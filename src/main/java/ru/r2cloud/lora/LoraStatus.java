package ru.r2cloud.lora;

import java.util.List;

import ru.r2cloud.model.DeviceConnectionStatus;

public class LoraStatus {

	private DeviceConnectionStatus deviceStatus;
	private String status;
	private List<ModulationConfig> configs;

	public LoraStatus() {
		// do nothing
	}

	public LoraStatus(DeviceConnectionStatus deviceStatus, String status) {
		this.status = status;
		this.deviceStatus = deviceStatus;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<ModulationConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<ModulationConfig> configs) {
		this.configs = configs;
	}

	public DeviceConnectionStatus getDeviceStatus() {
		return deviceStatus;
	}

	public void setDeviceStatus(DeviceConnectionStatus deviceStatus) {
		this.deviceStatus = deviceStatus;
	}
}
