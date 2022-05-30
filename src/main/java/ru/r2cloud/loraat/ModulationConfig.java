package ru.r2cloud.loraat;

public class ModulationConfig {

	private String name;
	private float minFrequency;
	private float maxFrequency;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getMinFrequency() {
		return minFrequency;
	}

	public void setMinFrequency(float minFrequency) {
		this.minFrequency = minFrequency;
	}

	public float getMaxFrequency() {
		return maxFrequency;
	}

	public void setMaxFrequency(float maxFrequency) {
		this.maxFrequency = maxFrequency;
	}

}
