package ru.r2cloud.model;

import java.io.File;
import java.util.Date;

public class ObservationResult {

	private String id;
	private File wavPath;
	private String aPath;
	private String bPath;
	private Date date;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public File getWavPath() {
		return wavPath;
	}
	
	public void setWavPath(File wavPath) {
		this.wavPath = wavPath;
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
