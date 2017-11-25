package ru.r2cloud.web;

import java.util.HashMap;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class ValidationResult extends HashMap<String, String> {

	private static final long serialVersionUID = 4094079248188757137L;

	private String general;

	public ValidationResult() {
		// do nothing
	}

	public ValidationResult(String general) {
		this.general = general;
	}

	public String getGeneral() {
		return general;
	}

	public void setGeneral(String general) {
		this.general = general;
	}

	@Override
	public boolean isEmpty() {
		if (general != null) {
			return false;
		}
		return super.isEmpty();
	}

	public static ValidationResult valueOf(String fieldName, String error) {
		ValidationResult result = new ValidationResult();
		result.put(fieldName, error);
		return result;
	}

	public String toJson() {
		JsonObject result = Json.object();
		if (!isEmpty()) {
			JsonObject errors = Json.object();
			if (general != null) {
				errors.add("general", general);
			}
			for (Entry<String, String> cur : entrySet()) {
				errors.add(cur.getKey(), cur.getValue());
			}
			result.add("errors", errors);
		}
		return result.toString();
	}

}
