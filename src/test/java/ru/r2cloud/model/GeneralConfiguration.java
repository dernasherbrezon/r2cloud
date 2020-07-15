package ru.r2cloud.model;

public class GeneralConfiguration {

	private Double lat;
	private Double lng;
	private boolean autoUpdate;
	private String ppmType;
	private Integer ppm;
	private Double elevationMin;
	private Double elevationGuaranteed;
	private boolean rotationEnabled;
	private String rotctrldHostname;
	private Integer rotctrldPort;
	private Double rotatorTolerance;
	private Long rotatorCycle;
	private Double gain;
	private boolean biast;

	public Double getGain() {
		return gain;
	}

	public void setGain(Double gain) {
		this.gain = gain;
	}

	public boolean isBiast() {
		return biast;
	}

	public void setBiast(boolean biast) {
		this.biast = biast;
	}

	public boolean isRotationEnabled() {
		return rotationEnabled;
	}

	public void setRotationEnabled(boolean rotationEnabled) {
		this.rotationEnabled = rotationEnabled;
	}

	public String getRotctrldHostname() {
		return rotctrldHostname;
	}

	public void setRotctrldHostname(String rotctrldHostname) {
		this.rotctrldHostname = rotctrldHostname;
	}

	public Integer getRotctrldPort() {
		return rotctrldPort;
	}

	public void setRotctrldPort(Integer rotctrldPort) {
		this.rotctrldPort = rotctrldPort;
	}

	public Double getRotatorTolerance() {
		return rotatorTolerance;
	}

	public void setRotatorTolerance(Double rotatorTolerance) {
		this.rotatorTolerance = rotatorTolerance;
	}

	public Long getRotatorCycle() {
		return rotatorCycle;
	}

	public void setRotatorCycle(Long rotatorCycle) {
		this.rotatorCycle = rotatorCycle;
	}

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
