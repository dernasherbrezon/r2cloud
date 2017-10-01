package ru.r2cloud.model;

import java.util.List;

public class WeatherSatellite {

	private Satellite satellite;
	private List<WeatherObservation> data;

	public Satellite getSatellite() {
		return satellite;
	}

	public void setSatellite(Satellite satellite) {
		this.satellite = satellite;
	}

	public List<WeatherObservation> getData() {
		return data;
	}

	public void setData(List<WeatherObservation> data) {
		this.data = data;
	}

}
