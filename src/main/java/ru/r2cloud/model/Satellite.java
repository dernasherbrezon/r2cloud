package ru.r2cloud.model;

public class Satellite {

	private String id;
	private String name;
	private long frequency;
	private String tleLine1;
	private String tleLine2;

	public String getTleLine1() {
		return tleLine1;
	}

	public void setTleLine1(String tleLine1) {
		this.tleLine1 = tleLine1;
	}

	public String getTleLine2() {
		return tleLine2;
	}

	public void setTleLine2(String tleLine2) {
		this.tleLine2 = tleLine2;
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
