package ru.r2cloud.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;

@SuppressWarnings("rawtypes")
public class Temperature extends FormattedGauge<Double> implements MetricSupplier<Gauge> {

	private static final Logger LOG = LoggerFactory.getLogger(Temperature.class);
	private final String fileLocation;

	public Temperature(String fileLocation) {
		super(MetricFormat.NORMAL);
		this.fileLocation = fileLocation;
	}

	@Override
	public Double getValue() {
		try (BufferedReader fis = new BufferedReader(new FileReader(fileLocation))) {
			String line = fis.readLine();
			if (line == null) {
				return null;
			}
			return ((double) Long.valueOf(line) / 1000);
		} catch (Exception e) {
			LOG.error("unable to get temp", e);
			return null;
		}
	}

	@Override
	public Gauge<?> newMetric() {
		return this;
	}

	public boolean isAvailable() {
		return new File(fileLocation).exists();
	}

}
