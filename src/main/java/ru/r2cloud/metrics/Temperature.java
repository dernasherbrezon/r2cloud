package ru.r2cloud.metrics;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;

@SuppressWarnings("rawtypes")
public class Temperature extends FormattedGauge<Double> implements MetricSupplier<Gauge> {

	private static final Logger LOG = LoggerFactory.getLogger(Temperature.class);
	private final Path fileLocation;

	public Temperature(Path fileLocation) {
		super(MetricFormat.NORMAL);
		this.fileLocation = fileLocation;
	}

	@Override
	public Double getValue() {
		try (BufferedReader fis = Files.newBufferedReader(fileLocation)) {
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
		return Files.exists(fileLocation);
	}

}
