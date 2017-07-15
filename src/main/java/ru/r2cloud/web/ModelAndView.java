package ru.r2cloud.web;

import java.util.HashMap;

public class ModelAndView extends HashMap<String, Object> {

	private static final long serialVersionUID = 7663044288968287447L;

	private String view;

	public ModelAndView() {
		// do nothing
	}

	public ModelAndView(String view) {
		this.view = view;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

}
