package ru.r2cloud.model;

public class GeneralConfiguration {

	private Double lat;
	private Double lng;
	private boolean autoUpdate;
	private String ppmType;
	private Integer ppm;
	private Double elevationMin;
	private Double elevationGuaranteed;
	
	public Double getElevationGuaranteed() {
		return elevationGuaranteed;
	}
	
	public void setElevationGuaranteed(Double elevationGuaranteed) {
		this.elevationGuaranteed = elevationGuaranteed;
	}
	
	public Double getElevationMin() {
		return elevationMin;
	}
	
	public void setElevationMin(Double elevationMin) {
		this.elevationMin = elevationMin;
	}

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

	public String getPpmType() {
		return ppmType;
	}

	public void setPpmType(String ppmType) {
		this.ppmType = ppmType;
	}

	public Integer getPpm() {
		return ppm;
	}

	public void setPpm(Integer ppm) {
		this.ppm = ppm;
	}

}
