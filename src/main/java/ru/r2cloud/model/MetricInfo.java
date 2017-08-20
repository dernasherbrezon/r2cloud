package ru.r2cloud.model;

import ru.r2cloud.metrics.MetricFormat;

public class MetricInfo {

	private String name;
	private MetricFormat format;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MetricFormat getFormat() {
		return format;
	}

	public void setFormat(MetricFormat format) {
		this.format = format;
	}

}
