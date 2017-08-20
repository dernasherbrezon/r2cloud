package ru.r2cloud.metrics;

import com.codahale.metrics.Counter;

public class FormattedCounter extends Counter {

	private final MetricFormat format;

	public FormattedCounter(MetricFormat format) {
		this.format = format;
	}

	public MetricFormat getFormat() {
		return format;
	}
	
}
