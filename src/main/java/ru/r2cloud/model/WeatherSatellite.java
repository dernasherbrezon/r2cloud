package ru.r2cloud.model;

import java.util.List;

public class WeatherSatellite {

	private Satellite satellite;
	private List<ObservationResult> data;

	public Satellite getSatellite() {
		return satellite;
	}

	public void setSatellite(Satellite satellite) {
		this.satellite = satellite;
	}

	public List<ObservationResult> getData() {
		return data;
	}

	public void setData(List<ObservationResult> data) {
		this.data = data;
	}

}
