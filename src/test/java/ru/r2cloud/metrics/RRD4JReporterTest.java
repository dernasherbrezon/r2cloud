package ru.r2cloud.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.util.Util;

import com.aerse.core.RrdDb;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class RRD4JReporterTest {

	private MetricRegistry registry;
	private RRD4JReporter reporter;
	private TestConfiguration config;
	private File basepath = new File("./target/rrd4jtest");
	
	@Test
	public void testCounterResumeFromLast() throws Exception {
		String name = UUID.randomUUID().toString();
		Counter c = registry.counter(name);
		c.inc(2);
		report(name, c);
		//simulate restart
		reporter.close();
		registry.remove(name);
		
		c = registry.counter(name);
		c.inc(1);
		Thread.sleep(1000);
		report(name, c);
		
		File f = new File(basepath, name + ".rrd");
		assertTrue(f.exists());
		RrdDb db = new RrdDb(f.getAbsolutePath());
		assertEquals(3.0, db.getLastDatasourceValue("data"), 0.0);
		db.close();
		c.inc(1);
		Thread.sleep(1000);
		report(name, c);
		
		db = new RrdDb(f.getAbsolutePath());
		assertEquals(4.0, db.getLastDatasourceValue("data"), 0.0);
		db.close();
	}

	@Test
	public void testSimpleCounter() throws Exception {
		String name = UUID.randomUUID().toString();
		Counter c = registry.counter(name);
		c.inc();
		report(name, c);

		File f = new File(basepath, name + ".rrd");
		assertTrue(f.exists());
		RrdDb db = new RrdDb(f.getAbsolutePath());
		assertEquals(1.0, db.getLastDatasourceValue("data"), 0.0);
		db.close();
	}

	@Before
	public void start() {
		registry = new MetricRegistry();
		config = new TestConfiguration();
		config.setProperty("metrics.basepath.location", "./target/rrd4jtest");
		reporter = new RRD4JReporter(config, registry);
	}

	@After
	public void stop() {
		config.stop();
		if (basepath.exists()) {
			Util.deleteDirectory(basepath);
		}
	}

	@SuppressWarnings("rawtypes")
	private void report(String name, Counter c) {
		TreeMap<String, Counter> counters = new TreeMap<String, Counter>();
		counters.put(name, c);
		reporter.report(new TreeMap<String, Gauge>(), counters, new TreeMap<String, Histogram>(), new TreeMap<String, Meter>(), new TreeMap<String, Timer>());
	}

}
