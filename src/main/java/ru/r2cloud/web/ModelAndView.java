package ru.r2cloud.web;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class ModelAndView extends HashMap<String, Object> {

	private static final long serialVersionUID = 7663044288968287447L;

	private String view;
	private MimeType type = MimeType.HTML;
	private Map<String, String> headers;
	private IStatus status;

	public ModelAndView() {
		// do nothing
	}

	public ModelAndView(String view) {
		this.view = view;
	}
	
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

	public MimeType getType() {
		return type;
	}

	public void setType(MimeType type) {
		this.type = type;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

}
