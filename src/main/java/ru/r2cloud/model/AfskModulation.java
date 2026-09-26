package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class AfskModulation {

	private long deviation;
	private long afCarrier;

	public AfskModulation() {
		// do nothing
	}

	public AfskModulation(AfskModulation other) {
		this.deviation = other.deviation;
		this.afCarrier = other.afCarrier;
	}

	public long getDeviation() {
		return deviation;
	}

	public void setDeviation(long deviation) {
		this.deviation = deviation;
	}

	public long getAfCarrier() {
		return afCarrier;
	}

	public void setAfCarrier(long afCarrier) {
		this.afCarrier = afCarrier;
	}

	public static AfskModulation fromJson(JsonObject asObject) {
		JsonValue afCarrier = asObject.get("afCarrier");
		if (afCarrier == null) {
			return null;
		}
		AfskModulation result = new AfskModulation();
		result.setAfCarrier(afCarrier.asLong());

		JsonValue deviation = asObject.get("deviation");
		if (deviation != null) {
			result.setDeviation(deviation.asLong());
		} else {
			result.setDeviation(5000);
		}
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.set("deviation", deviation);
		result.set("afCarrier", afCarrier);
		return result;
	}
}
