package ru.r2cloud.model;

public class Page {

	private String cursor;
	private Integer limit;
	private String satelliteId;

	public Page() {
		// do nothing
	}

	public Page(Integer limit) {
		this.limit = limit;
	}

	public Page(Integer limit, String cursor) {
		this.limit = limit;
		this.cursor = cursor;
	}

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		if (cursor != null && cursor.trim().length() == 0) {
			this.cursor = null;
		} else {
			this.cursor = cursor;
		}
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		if (satelliteId != null && satelliteId.trim().length() == 0) {
			this.satelliteId = null;
		} else {
			this.satelliteId = satelliteId;
		}
	}

}
