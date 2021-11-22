package ru.r2cloud.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.core.RrdDb;
import com.aerse.core.Sample;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ru.r2cloud.ManualClock;
import ru.r2cloud.TestConfiguration;

public class RRD4JReporterTest {

	private MetricRegistry registry;
	private RRD4JReporter reporter;
	private File basepath;
	private ManualClock clock;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testDoNotUpdateMetricIfClockJumpedBackwards() throws Exception {
		String name = UUID.randomUUID().toString();
		Counter c = registry.counter(name);
		c.inc();
		report(name, c);

		// ensure internal RrdDb are closed
		reporter.close();

		// simulate sample in far future
		double lastSample = 15.0;
		RrdDb db = getRrdDb(name);
		Sample sample = db.createSample(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1L));
		sample.setValue("data", lastSample);
		sample.update();
		db.close();

		c.inc();
		clock.add(1000);
		report(name, c);

		assertEquals(lastSample, readLastValueFromFile(name), 0.0);
	}

	@Test
	public void testCounterResumeFromLast() throws Exception {
		String name = UUID.randomUUID().toString();
		Counter c = registry.counter(name);
		c.inc(2);
		report(name, c);
		// simulate restart
		reporter.close();
		registry.remove(name);

		c = registry.counter(name);
		c.inc(1);
		clock.add(1000);
		report(name, c);

		assertEquals(3.0, readLastValueFromFile(name), 0.0);
		c.inc(1);

		clock.add(1000);
		report(name, c);

		assertEquals(4.0, readLastValueFromFile(name), 0.0);
	}

	@Test
	public void testSimpleCounter() throws Exception {
		String name = UUID.randomUUID().toString();
		Counter c = registry.counter(name);
		c.inc();
		report(name, c);

		assertEquals(1.0, readLastValueFromFile(name), 0.0);
	}

	@Before
	public void start() throws Exception {
		registry = new MetricRegistry();
		TestConfiguration config = new TestConfiguration(tempFolder);
		basepath = new File(tempFolder.getRoot().getAbsolutePath(), "rrd4jtest");
		config.setProperty("metrics.basepath.location", basepath.getAbsolutePath());
		clock = new ManualClock();
		reporter = new RRD4JReporter(config, registry, clock);
	}

	@SuppressWarnings("rawtypes")
	private void report(String name, Counter c) {
		TreeMap<String, Counter> counters = new TreeMap<>();
		counters.put(name, c);
		reporter.report(new TreeMap<String, Gauge>(), counters, new TreeMap<String, Histogram>(), new TreeMap<String, Meter>(), new TreeMap<String, Timer>());
	}

	private double readLastValueFromFile(String name) throws Exception {
		RrdDb db = getRrdDb(name);
		double result = db.getLastDatasourceValue("data");
		db.close();
		return result;
	}

	private RrdDb getRrdDb(String name) throws Exception {
		File f = new File(basepath, name + ".rrd");
		assertTrue(f.exists());
		return new RrdDb(f.getAbsolutePath());
	}

}
