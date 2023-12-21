package ru.r2cloud.lora;

public class ModulationConfig {

	private String name;
	private long minFrequency;
	private long maxFrequency;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
