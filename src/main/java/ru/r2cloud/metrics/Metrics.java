package ru.r2cloud.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.util.Configuration;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;

public class Metrics {

	private static final Logger LOG = LoggerFactory.getLogger(R2Cloud.class);
	public static final MetricRegistry REGISTRY = SharedMetricRegistries.getOrCreate("r2cloud");
	public static final HealthCheckRegistry HEALTH_REGISTRY = SharedHealthCheckRegistries.getOrCreate("r2cloud");

	private RRD4JReporter reporter;
	private Sigar sigar;
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
					return new FormattedGauge<Double>(MetricFormat.NORMAL) {

						@Override
						public Double getValue() {
							try (BufferedReader fis = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"))) {
								String line = fis.readLine();
								if (line == null) {
									return null;
								}
								return ((double) Long.valueOf(line) / 1000);
							} catch (Exception e) {
								LOG.error("unable to get temp: " + e.getMessage());
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
				return new FormattedGauge<Long>(MetricFormat.BYTES) {

					@Override
					public Long getValue() {
						return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					}
				};
			}
		});

		try {
			sigar = new Sigar();
			File f = sigar.getNativeLibrary();
			if (f != null && f.exists()) {
				REGISTRY.gauge("load-average", new MetricSupplier<Gauge>() {
					@Override
					public Gauge<?> newMetric() {
						return new FormattedGauge<Double>(MetricFormat.NORMAL) {

							@Override
							public Double getValue() {
								try {
									return sigar.getLoadAverage()[0];
								} catch (SigarException e) {
									return null;
								}
							}
						};
					}
				});
				REGISTRY.gauge("ram-used", new MetricSupplier<Gauge>() {
					@Override
					public Gauge<?> newMetric() {
						return new FormattedGauge<Double>(MetricFormat.NORMAL) {

							@Override
							public Double getValue() {
								try {
									return sigar.getMem().getUsedPercent();
								} catch (SigarException e) {
									return null;
								}
							}
						};
					}
				});
				LOG.info("SIGAR library was loaded");
			}
		} catch (UnsatisfiedLinkError linkError) {
			LOG.info("Could not initialize SIGAR library: " + linkError.getMessage());
		}

		reporter = new RRD4JReporter(config, REGISTRY);
		reporter.start();
		LOG.info("metrics started");
	}

	public void stop() {
		if (sigar != null) {
			sigar.close();
		}
		if (reporter != null) {
			reporter.close();
		}
	}
}
