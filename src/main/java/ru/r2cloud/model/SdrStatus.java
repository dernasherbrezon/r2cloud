package ru.r2cloud.model;

public class SdrStatus {

	private String error;
	private boolean dongleConnected;

	public void setDongleConnected(boolean dongleConnected) {
		this.dongleConnected = dongleConnected;
	}

	public boolean isDongleConnected() {
		return dongleConnected;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "SdrStatus [error=" + error + ", dongleConnected=" + dongleConnected + "]";
	}

}
