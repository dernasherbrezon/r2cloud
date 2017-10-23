package ru.r2cloud.model;

import java.util.Date;

public class Satellite {

	private String id;
	private String name;
	private long frequency;
	private Date nextPass;
	
	public Date getNextPass() {
		return nextPass;
	}
	
	public void setNextPass(Date nextPass) {
		this.nextPass = nextPass;
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
