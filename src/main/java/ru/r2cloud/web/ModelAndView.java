package ru.r2cloud.web;

import java.util.HashMap;
import java.util.Map;

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

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public void addHeader(String name, String value) {
		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		headers.put(name, value);
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
