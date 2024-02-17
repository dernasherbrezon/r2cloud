package ru.r2cloud.model;

public class AntennaConfiguration {

	private AntennaType type;
	// omnidirectional and directional with rotator
	private double minElevation;
	private double guaranteedElevation;
	// fixed directional
	private double azimuth;
	private double elevation;

	public AntennaType getType() {
		return type;
	}

	public void setType(AntennaType type) {
		this.type = type;
	}

	public double getMinElevation() {
		return minElevation;
	}

	public void setMinElevation(double minElevation) {
		this.minElevation = minElevation;
	}

	public double getGuaranteedElevation() {
		return guaranteedElevation;
	}

	public void setGuaranteedElevation(double guaranteedElevation) {
		this.guaranteedElevation = guaranteedElevation;
	}

	public double getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
	}

	public double getElevation() {
		return elevation;
	}

	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

}
