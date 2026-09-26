package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class BpskModulation {

	private boolean bpskDifferential;
	private double bpskCenterFrequency;

	public BpskModulation() {
		// do nothing
	}

	public BpskModulation(BpskModulation other) {
		this.bpskCenterFrequency = other.bpskCenterFrequency;
		this.bpskDifferential = other.bpskDifferential;
	}

	public boolean isBpskDifferential() {
		return bpskDifferential;
	}

	public void setBpskDifferential(boolean bpskDifferential) {
		this.bpskDifferential = bpskDifferential;
	}

	public double getBpskCenterFrequency() {
		return bpskCenterFrequency;
	}

	public void setBpskCenterFrequency(double bpskCenterFrequency) {
		this.bpskCenterFrequency = bpskCenterFrequency;
	}

	public static BpskModulation fromJson(JsonObject asObject) {
		BpskModulation result = new BpskModulation();
		JsonValue bpskCenterFrequency = asObject.get("bpskCenterFrequency");
		if (bpskCenterFrequency != null) {
			result.setBpskCenterFrequency(bpskCenterFrequency.asDouble());
		}
		JsonValue bpskDifferential = asObject.get("bpskDifferential");
		if (bpskDifferential != null) {
			result.setBpskDifferential(bpskDifferential.asBoolean());
		}
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.add("bpskCenterFrequency", bpskCenterFrequency);
		result.add("bpskDifferential", bpskDifferential);
		return result;
	}
}
