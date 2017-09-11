package ru.r2cloud.model;

import ru.r2cloud.util.Configuration;

public class ConfigurationBean {

	private String lat;
	private String lon;

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	public static ConfigurationBean fromConfig(Configuration props) {
		ConfigurationBean entity = new ConfigurationBean();
		entity.setLat(props.getProperty("locaiton.lat"));
		entity.setLon(props.getProperty("locaiton.lon"));
		return entity;
	}
}
