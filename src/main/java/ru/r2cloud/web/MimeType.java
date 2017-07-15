package ru.r2cloud.web;

public enum MimeType {
	
	HTML("text/html"), JSON("application/json");
	
	private String type;
	
	private MimeType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}

}
