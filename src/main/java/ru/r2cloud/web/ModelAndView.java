package ru.r2cloud.web;

import java.util.Map;

import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class ModelAndView {

	private String data;
	private Map<String, String> headers;
	private IStatus status;

	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus status) {
		this.status = status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setData(JsonValue data) {
		this.data = data.toString();
	}

}
