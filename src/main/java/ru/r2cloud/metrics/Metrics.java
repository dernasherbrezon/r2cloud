package ru.r2cloud.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.uitl.Configuration;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.SharedMetricRegistries;

public class Metrics {

	private static final Logger LOG = Logger.getLogger(R2Cloud.class.getName());
	public static final MetricRegistry REGISTRY = SharedMetricRegistries.getOrCreate("r2cloud");

	private RRD4JReporter reporter;
	private final Configuration config;

	public Metrics(Configuration config) {
		this.config = config;
	}

	@SuppressWarnings("rawtypes")
	public void start() {
		if (new File("/sys/class/thermal/thermal_zone0/temp").exists()) {
			REGISTRY.gauge("temperature", new MetricSupplier<Gauge>() {

				@Override
				public Gauge<?> newMetric() {
					return new Gauge<Double>() {
						@Override
						public Double getValue() {
							try (BufferedReader fis = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"))) {
								String line = fis.readLine();
								if (line == null) {
									return null;
								}
								return ((double) Long.valueOf(line) / 1000);
							} catch (Exception e) {
								LOG.log(Level.SEVERE, "unable to get temp: " + e.getMessage());
								return null;
							}
						}
					};
				}
			});
		} else {
			LOG.info("temperature metric is not available");
		}

		REGISTRY.gauge("heap", new MetricSupplier<Gauge>() {
			@Override
			public Gauge<?> newMetric() {
				return new Gauge<Long>() {
					@Override
					public Long getValue() {
						return Runtime.getRuntime().totalMemory();
					}
				};
			}
		});

		reporter = new RRD4JReporter(config, REGISTRY);
		reporter.start();
		LOG.info("metrics started");
	}

	public void stop() {
		if (reporter != null) {
			reporter.close();
		}
	}
}
