package ru.r2cloud.model;

import java.util.Date;

public class ObservationResult {

	private String aPath;
	private String bPath;
	private Date date;
	private String waterfall;
	
	public String getWaterfall() {
		return waterfall;
	}
	
	public void setWaterfall(String waterfall) {
		this.waterfall = waterfall;
	}
	
	public String getaPath() {
		return aPath;
	}

	public void setaPath(String aPath) {
		this.aPath = aPath;
	}

	public String getbPath() {
		return bPath;
	}

	public void setbPath(String bPath) {
		this.bPath = bPath;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
