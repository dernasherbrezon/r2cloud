package ru.r2cloud.web;

import java.util.HashMap;

public class ModelAndView extends HashMap<String, Object> {

	private static final long serialVersionUID = 7663044288968287447L;

	private String view;
	private MimeType type = MimeType.HTML;

	public ModelAndView() {
		// do nothing
	}

	public ModelAndView(String view) {
		this.view = view;
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
