package ru.r2cloud.web;

import java.util.HashMap;

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

}
