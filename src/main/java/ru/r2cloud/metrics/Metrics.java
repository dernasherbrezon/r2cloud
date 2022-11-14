package ru.r2cloud.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;

import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;

public class Metrics {

	private static final Logger LOG = LoggerFactory.getLogger(Metrics.class);
	private final MetricRegistry registry = SharedMetricRegistries.getOrCreate("r2cloud");
	private final HealthCheckRegistry healthRegistry = SharedHealthCheckRegistries.getOrCreate("r2cloud");

	private JmxReporter jmxReporter;
	private RRD4JReporter reporter;
	private final Configuration config;
	private final Clock clock;

	public Metrics(Configuration config, Clock clock) {
		this.config = config;
		this.clock = clock;
	}

	public void start() {
		Temperature temp = new Temperature(config.getPath("/sys/class/thermal/thermal_zone0/temp"));
		if (temp.isAvailable()) {
			registry.gauge("temperature", temp);
		} else {
			LOG.info("temperature metric is not available");
		}

		LOG.info("max memory: {}", Runtime.getRuntime().maxMemory());
		LOG.info("total memory: {}", Runtime.getRuntime().totalMemory());
		LOG.info("CPU count: {}", Runtime.getRuntime().availableProcessors());

		registry.gauge("heap", new MetricSupplier<>() {
			@Override
			public Gauge<Long> newMetric() {
				return new FormattedGauge<Long>(MetricFormat.BYTES) {

					@Override
					public Long getValue() {
						return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					}
				};
			}
		});

		try {
			jmxReporter = JmxReporter.forRegistry(registry).inDomain("ru.r2cloud.metrics").build();
			jmxReporter.start();
		} catch (NoClassDefFoundError e) {
			// ignore missing java.management module on old r2cloud-jdk
		}

		reporter = new RRD4JReporter(config, registry, clock);
		reporter.start();
		reporter.report();
		LOG.info("metrics started");
	}

	public void stop() {
		if (reporter != null) {
			reporter.close();
		}
		if (jmxReporter != null) {
			jmxReporter.close();
		}
		SharedHealthCheckRegistries.remove("r2cloud");
		SharedMetricRegistries.remove("r2cloud");
	}

	public MetricRegistry getRegistry() {
		return registry;
	}

	public HealthCheckRegistry getHealthRegistry() {
		return healthRegistry;
	}
}
