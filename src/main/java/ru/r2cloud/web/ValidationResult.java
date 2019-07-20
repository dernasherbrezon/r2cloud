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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((general == null) ? 0 : general.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidationResult other = (ValidationResult) obj;
		if (general == null) {
			if (other.general != null) {
				return false;
			}
		} else if (!general.equals(other.general)) {
			return false;
		}
		return true;
	}

}
