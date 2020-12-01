package ru.r2cloud.metrics;

import com.codahale.metrics.Gauge;

public abstract class FormattedGauge<T> implements Gauge<T> {

	private final MetricFormat format;

	protected FormattedGauge(MetricFormat format) {
		this.format = format;
	}

	public MetricFormat getFormat() {
		return format;
	}

}
