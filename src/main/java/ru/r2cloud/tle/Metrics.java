package ru.r2cloud.tle;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.cloud.InfluxDBClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class Metrics {

	private ScheduledExecutorService executor = null;

	private final Configuration config;
	private final ThreadPoolFactory threadFactory;
	private final InfluxDBClient client;

	public Metrics(Configuration config, ThreadPoolFactory threadFactory, InfluxDBClient client) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.client = client;
	}

	public void start() {
		long periodMillis = config.getLong("influxdb.reportMillis");
		executor = threadFactory.newScheduledThreadPool(1, new NamingThreadFactory("influxdb-reporter"));
		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				client.sendJvm();
			}
		}, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

}
