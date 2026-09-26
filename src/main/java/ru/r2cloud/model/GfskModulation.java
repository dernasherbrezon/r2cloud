package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class GfskModulation {

	private long deviation;
	private float bt;
	private double transitionWidth;

	public GfskModulation() {
		// do nothing
	}

	public GfskModulation(GfskModulation other) {
		this.deviation = other.deviation;
		this.bt = other.bt;
		this.transitionWidth = other.transitionWidth;
	}

	public double getTransitionWidth() {
		return transitionWidth;
	}

	public void setTransitionWidth(double transitionWidth) {
		this.transitionWidth = transitionWidth;
	}

	public long getDeviation() {
		return deviation;
	}

	public void setDeviation(long deviation) {
		this.deviation = deviation;
	}

	public float getBt() {
		return bt;
	}

	public void setBt(float bt) {
		this.bt = bt;
	}

	public static GfskModulation fromJson(JsonObject asObject) {
		GfskModulation result = new GfskModulation();
		JsonValue deviation = asObject.get("deviation");
		if (deviation != null) {
			result.setDeviation(deviation.asLong());
		} else {
			result.setDeviation(5000);
		}
		JsonValue bt = asObject.get("bt");
		if (bt != null) {
			result.setBt(bt.asFloat());
		} else {
			result.setBt(0.5f); // GMSK by default
		}
		JsonValue transitionWidth = asObject.get("transitionWidth");
		if (transitionWidth != null) {
			result.setTransitionWidth(transitionWidth.asDouble());
		} else {
			result.setTransitionWidth(2000);
		}
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.set("deviation", deviation);
		result.set("bt", bt);
		result.set("transitionWidth", transitionWidth);
		return result;
	}
}
