package ru.r2cloud.model;

public class Satellite {

	private String noradCatId;
	private String name;
	private long frequency;
	
	public long getFrequency() {
		return frequency;
	}
	
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public String getNoradCatId() {
		return noradCatId;
	}

	public void setNoradCatId(String noradCatId) {
		this.noradCatId = noradCatId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
