package ru.r2cloud.r2lora;

import java.util.List;

public class R2loraStatus {

	private String status;
	private int chipTemperature;
	private List<ModulationConfig> configs;

	public R2loraStatus() {
		// do nothing
	}

	public R2loraStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getChipTemperature() {
		return chipTemperature;
	}

	public void setChipTemperature(int chipTemperature) {
		this.chipTemperature = chipTemperature;
	}

	public List<ModulationConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<ModulationConfig> configs) {
		this.configs = configs;
	}

}
