package ru.r2cloud.model;


public class Satellite {

	private String id;
	private String name;
	private String decoder;
	private long frequency;
	private SatelliteType type;
	
	public SatelliteType getType() {
		return type;
	}
	
	public void setType(SatelliteType type) {
		this.type = type;
	}
	
	public String getDecoder() {
		return decoder;
	}
	
	public void setDecoder(String decoder) {
		this.decoder = decoder;
	}
	
	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
