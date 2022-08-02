package ru.r2cloud.model;

import java.util.Arrays;

import com.eclipsesource.json.JsonObject;

public class Tle {

	private final String[] raw;
	private long lastUpdateTime;

	public Tle(String[] tle) {
		this.raw = tle;
	}

	public String[] getRaw() {
		return raw;
	}
	
	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
	
	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(raw);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tle other = (Tle) obj;
		return Arrays.equals(raw, other.raw);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (raw.length > 0) {
			json.add("line1", raw[0]);
		}
		if (raw.length > 1) {
			json.add("line2", raw[1]);
		}
		if (raw.length > 2) {
			json.add("line3", raw[2]);
		}
		return json;
	}

	public static Tle fromJson(JsonObject json) {
		String[] raw = new String[3];
		raw[0] = json.getString("line1", null);
		raw[1] = json.getString("line2", null);
		raw[2] = json.getString("line3", null);
		return new Tle(raw);
	}

}
