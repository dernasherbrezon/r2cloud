package ru.r2cloud.metrics;

import java.io.File;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;

import ru.r2cloud.cloud.R2ServerService;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;

public class Metrics {

	private static final Logger LOG = LoggerFactory.getLogger(Metrics.class);
	private final MetricRegistry registry = SharedMetricRegistries.getOrCreate("r2cloud");
	private final HealthCheckRegistry healthRegistry = SharedHealthCheckRegistries.getOrCreate("r2cloud");

	private RRD4JReporter reporter;
	private Sigar sigar;
	private final Configuration config;
	private final R2ServerService cloudService;
	private final Clock clock;

	public Metrics(Configuration config, R2ServerService cloudService, Clock clock) {
		this.config = config;
		this.cloudService = cloudService;
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
			sigar = new Sigar();
			File f = sigar.getNativeLibrary();
			if (f != null && f.exists()) {
				registry.gauge("load-average", new MetricSupplier<>() {
					@Override
					public Gauge<Double> newMetric() {
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
				registry.gauge("ram-used", new MetricSupplier<>() {
					@Override
					public Gauge<Double> newMetric() {
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
				registry.gauge("disk-used", new MetricSupplier<>() {
					@Override
					public Gauge<Double> newMetric() {
						return new FormattedGauge<Double>(MetricFormat.NORMAL) {

							@Override
							public Double getValue() {
								try {
									return sigar.getFileSystemUsage("/").getUsePercent() * 100;
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
			LOG.info("Could not initialize SIGAR library: {}", linkError.getMessage());
		}

		reporter = new RRD4JReporter(config, registry, cloudService, clock);
		reporter.start();
		reporter.report();
		LOG.info("metrics started");
	}

	public void stop() {
		if (sigar != null) {
			sigar.close();
		}
		if (reporter != null) {
			reporter.close();
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
