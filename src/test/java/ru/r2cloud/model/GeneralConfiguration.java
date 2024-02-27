package ru.r2cloud.model;

public class GeneralConfiguration {

	private Double lat;
	private Double lng;
	private boolean autoUpdate;
	private boolean presentationMode;

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}

	public boolean isPresentationMode() {
		return presentationMode;
	}

	public void setPresentationMode(boolean presentationMode) {
		this.presentationMode = presentationMode;
	}

}
