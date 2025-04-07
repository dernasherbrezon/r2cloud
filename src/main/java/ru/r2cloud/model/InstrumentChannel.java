package ru.r2cloud.model;

import com.eclipsesource.json.JsonObject;

public class InstrumentChannel {

	private String id;
	private String description;

	public InstrumentChannel() {
		// do nothing
	}

	public InstrumentChannel(InstrumentChannel other) {
		this.id = other.id;
		this.description = other.description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static InstrumentChannel fromJson(JsonObject obj) {
		InstrumentChannel result = new InstrumentChannel();
		result.setId(obj.getString("id", null));
		if (result.getId() == null) {
			return null;
		}
		result.setDescription(obj.getString("description", null));
		return result;
	}

}
