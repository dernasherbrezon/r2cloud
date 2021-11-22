package ru.r2cloud.metrics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerse.ConsolFun;
import com.aerse.DsType;
import com.aerse.core.RrdBackendFactory;
import com.aerse.core.RrdDb;
import com.aerse.core.RrdDef;
import com.aerse.core.Sample;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class RRD4JReporter extends ScheduledReporter {

	private static final Logger LOG = LoggerFactory.getLogger(RRD4JReporter.class);
	private static final long STEP = 300; // secs

	private final File basepath;
	private final Map<String, RrdDb> dbPerMetric = new HashMap<>();
	private final Map<String, Double> lastValueForCounter = new HashMap<>();
	private final Clock clock;

	RRD4JReporter(Configuration config, MetricRegistry registry, Clock clock) {
		super(registry, "rrd4j-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
		basepath = Util.initDirectory(config.getProperty("metrics.basepath.location"));
		this.clock = clock;
	}

	// never change the step. this will break previously created rrd files
	public void start() {
		start(STEP, TimeUnit.SECONDS);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		JsonArray metricsToShare = new JsonArray();
		for (Entry<String, Gauge> cur : gauges.entrySet()) {
			Object value = cur.getValue().getValue();
			if (value == null) {
				continue;
			}
			metricsToShare.add(convertGauge(cur));
		}
		for (Entry<String, Counter> cur : counters.entrySet()) {
			metricsToShare.add(convertCounter(cur));
		}
	}

	private JsonObject convertCounter(Entry<String, Counter> cur) {
		long newValue = cur.getValue().getCount();
		// split method getOrCreate to retrive lastDatasourceValue only once
		// if newvalue is less than lastDatasource value, then jvm was
		// restarted
		// add lastValue to avoid huge spikes in graphs after application
		// restarts/upgrades
		RrdDb result = dbPerMetric.get(cur.getKey());
		if (result == null) {
			result = create(cur.getKey(), DsType.COUNTER, ConsolFun.AVERAGE);
			if (result != null) {
				try {
					double lastValue = result.getLastDatasourceValue("data");
					if (!Double.isNaN(lastValue)) {
						lastValueForCounter.put(cur.getKey(), lastValue);
					}
				} catch (IOException e) {
					LOG.error("unable to load last value", e);
				}
			}
		}
		Double lastValue = lastValueForCounter.get(cur.getKey());
		if (lastValue != null) {
			newValue += lastValue;
		}
		update(result, newValue);
		JsonObject metric = new JsonObject();
		metric.add("name", cur.getKey());
		metric.add("value", newValue);
		return metric;
	}

	@SuppressWarnings("rawtypes")
	private JsonObject convertGauge(Entry<String, Gauge> cur) {
		Object value = cur.getValue().getValue();
		update(getOrCreate(cur.getKey(), DsType.GAUGE, ConsolFun.AVERAGE), convertToDouble(value));
		JsonObject metric = new JsonObject();
		metric.add("name", cur.getKey());
		metric.add("value", convertToDouble(value));
		return metric;
	}

	private static double convertToDouble(Object value) {
		if (value instanceof Double) {
			return (Double) value;
		}
		if (value instanceof Long) {
			return ((Long) value).doubleValue();
		}
		if (value instanceof Integer) {
			return ((Integer) value).doubleValue();
		}
		throw new IllegalArgumentException("unsupported value type: " + value.getClass());
	}

	private void update(RrdDb db, double value) {
		if (db == null) {
			return;
		}
		Sample sample;
		try {
			sample = db.createSample((clock.millis() + 500L) / 1000L);
		} catch (IOException e) {
			LOG.error("unable to create sample", e);
			return;
		}
		sample.setValue("data", value);
		try {
			sample.update();
		} catch (IOException e) {
			LOG.error("unable to update", e);
		} catch (IllegalArgumentException e) {
			// time in raspberrypi can jump back and forward.
			// just ignore new metrics, until time corrected
		}
	}

	private RrdDb getOrCreate(String metricName, DsType type, ConsolFun fun) {
		RrdDb result = dbPerMetric.get(metricName);
		if (result != null) {
			return result;
		}
		return create(metricName, type, fun);
	}

	private RrdDb create(String metricName, DsType type, ConsolFun fun) {
		RrdDb result;
		File path = new File(basepath, metricName + ".rrd");
		if (!path.exists()) {
			RrdDef rrdDef = new RrdDef(new File(basepath, metricName + ".rrd").getAbsolutePath(), System.currentTimeMillis() / 1000 - 1, STEP);
			rrdDef.setVersion(2);
			rrdDef.addDatasource("data", type, 2 * STEP, 0, Double.NaN);
			rrdDef.addArchive(fun, 0.5, 1, 600); // ~last 2 days.
													// each point
			// is a 300 seconds
			rrdDef.addArchive(fun, 0.5, 24, 775); // ~last 2
													// months.
			rrdDef.addArchive(fun, 0.5, 288, 797); // ~last 2
													// years.
													// each
			// point is a day
			try {
				result = new RrdDb(rrdDef);
			} catch (IOException e) {
				LOG.error("unable to create database: {}", path.getAbsolutePath(), e);
				return null;
			}
		} else {
			try {
				result = new RrdDb(path.getAbsolutePath(), RrdBackendFactory.getFactory("FILE"));
			} catch (IOException e) {
				LOG.error("unable to load database: {}", path.getAbsolutePath(), e);
				return null;
			}
		}

		RrdDb old = dbPerMetric.put(metricName, result);
		if (old != null) {
			LOG.error("found duplicate rrddb: {}", metricName);
			try {
				old.close();
			} catch (IOException e) {
				LOG.error("unable to close: {}", old.getPath(), e);
			}
		}

		return result;
	}

	@Override
	public void close() {
		super.close();
		for (RrdDb cur : dbPerMetric.values()) {
			try {
				cur.close();
			} catch (IOException e) {
				LOG.error("unable to close: {}", cur.getPath(), e);
			}
		}
		dbPerMetric.clear();
	}

}
