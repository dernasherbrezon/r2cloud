package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;

public class AntennaConfiguration {

	private AntennaType type;
	// omnidirectional and directional with rotator
	private double minElevation;
	private double guaranteedElevation;
	// fixed directional
	private double azimuth;
	private double elevation;
	private double beamwidth;

	public double getBeamwidth() {
		return beamwidth;
	}

	public void setBeamwidth(double beamwidth) {
		this.beamwidth = beamwidth;
	}

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

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.add("antennaType", type.name());
		json.add("azimuth", azimuth);
		json.add("elevation", elevation);
		json.add("beamwidth", beamwidth);
		json.add("minElevation", minElevation);
		json.add("guaranteedElevation", guaranteedElevation);
		return json;
	}

	public static AntennaConfiguration fromJson(JsonObject meta) {
		AntennaConfiguration result = new AntennaConfiguration();
		result.setType(AntennaType.valueOf(meta.getString("antennaType", "OMNIDIRECTIONAL")));
		result.setAzimuth(meta.getDouble("azimuth", 0));
		result.setElevation(meta.getDouble("elevation", 0));
		result.setBeamwidth(meta.getDouble("beamwidth", 0));
		result.setMinElevation(meta.getDouble("minElevation", 0));
		result.setGuaranteedElevation(meta.getDouble("guaranteedElevation", 0));
		return result;
	}

}
