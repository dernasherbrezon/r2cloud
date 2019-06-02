package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.health.HealthCheck;

import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.MetricFormat;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.RtlSdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class RtlSdrStatusDao implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(RtlSdrStatusDao.class);
	private static final Pattern DEVICEPATTERN = Pattern.compile("^  0:  (.*?), (.*?), SN: (.*?)$");
	private static final Pattern PPMPATTERN = Pattern.compile("real sample rate: \\d+ current PPM: \\d+ cumulative PPM: (\\d+)");

	private final Configuration config;
	private final RtlSdrLock lock;
	private final ThreadPoolFactory threadpoolFactory;
	private final Metrics metrics;

	private ScheduledExecutorService executor;
	private RtlSdrStatus status;

	private Integer currentPpm;

	public RtlSdrStatusDao(Configuration config, RtlSdrLock lock, ThreadPoolFactory threadpoolFactory, Metrics metrics) {
		this.config = config;
		this.lock = lock;
		this.threadpoolFactory = threadpoolFactory;
		this.metrics = metrics;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized void start() {
		if (executor != null) {
			return;
		}
		currentPpm = config.getInteger("ppm.current");
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		executor.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				synchronized (RtlSdrStatusDao.this) {
					status = getStatus();
				}
			}
		}, 0, config.getLong("rtltest.interval.seconds"), TimeUnit.SECONDS);
		if (config.getBoolean("ppm.calculate")) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			try {
				Date date = sdf.parse(config.getProperty("ppm.calculate.timeUTC"));
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				Calendar executeAt = Calendar.getInstance();
				executeAt.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
				executeAt.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
				executeAt.set(Calendar.SECOND, 0);
				executeAt.set(Calendar.MILLISECOND, 0);
				long current = System.currentTimeMillis();
				if (executeAt.getTimeInMillis() < current) {
					executeAt.add(Calendar.DAY_OF_MONTH, 1);
				}
				LOG.info("next ppm execution at: {}", executeAt.getTime());
				executor.scheduleAtFixedRate(new Runnable() {

					@Override
					public void run() {
						Integer ppm = getPpm();
						if (ppm == null) {
							return;
						}
						synchronized (RtlSdrStatusDao.this) {
							currentPpm = ppm;
						}
						config.setProperty("ppm.current", ppm);
						config.update();
					}
				}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);

			} catch (ParseException e) {
				LOG.info("invalid time. ppm will be disabled", e);
			}
		}
		metrics.getHealthRegistry().register("rtltest", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				synchronized (RtlSdrStatusDao.this) {
					if (status == null || !status.isDongleConnected()) {
						return ResultUtil.unknown();
					}
					if (status.getError() == null) {
						return ResultUtil.healthy();
					} else {
						return ResultUtil.unhealthy(status.getError());
					}
				}
			}
		});
		metrics.getHealthRegistry().register("rtldongle", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				synchronized (RtlSdrStatusDao.this) {
					if (status == null) {
						return ResultUtil.unknown();
					}
					if (status.isDongleConnected()) {
						return ResultUtil.healthy();
					} else {
						return ResultUtil.unhealthy("No supported devices found");
					}
				}
			}
		});
		metrics.getRegistry().gauge("ppm", new MetricSupplier<Gauge>() {

			@Override
			public Gauge<?> newMetric() {
				return new FormattedGauge<Integer>(MetricFormat.NORMAL) {

					@Override
					public Integer getValue() {
						// graph will be displayed anyway.
						// fill it with 0
						synchronized (RtlSdrStatusDao.this) {
							if (currentPpm == null) {
								return 0;
							}
							return currentPpm;
						}
					}
				};
			}
		});

		LOG.info("started");
	}

	@Override
	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
		LOG.info("stopped");
	}

	Integer getPpm() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("ppm calculation");
		}
		if (!lock.tryLock(this)) {
			LOG.info("unable to lock for ppm calculation");
			return null;
		}

		Process rtlTest = null;
		Integer result = null;
		try {
			rtlTest = new ProcessBuilder().command(config.getProperty("stdbuf.path"), "-i", "0", "-o", "0", "-e", "0", config.getProperty("rtltest.path"), "-p2").redirectErrorStream(true).start();
			BufferedReader r = new BufferedReader(new InputStreamReader(rtlTest.getInputStream()));
			String curLine = null;
			int numberOfSamples = 0;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(curLine);
				}
				if (curLine.startsWith("No supported")) {
					break;
				} else if (curLine.startsWith("real sample rate")) {
					numberOfSamples++;
					if (numberOfSamples >= 10) {
						Matcher m = PPMPATTERN.matcher(curLine);
						if (m.find()) {
							String ppmStr = m.group(1);
							result = Integer.valueOf(ppmStr);
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			LOG.error("unable to calculate ppm", e);
		} finally {
			Util.shutdown("ppm-test", rtlTest, 5000);
			lock.unlock(this);
		}
		return result;
	}

	RtlSdrStatus getStatus() {
		RtlSdrStatus status = null;
		try {
			Process rtlTest = new ProcessBuilder().command(config.getProperty("rtltest.path"), "-t").start();
			BufferedReader r = new BufferedReader(new InputStreamReader(rtlTest.getErrorStream()));
			String curLine = null;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.startsWith("No supported")) {
					status = new RtlSdrStatus();
					status.setDongleConnected(false);
					break;
				} else {
					Matcher m = DEVICEPATTERN.matcher(curLine);
					if (m.find()) {
						status = new RtlSdrStatus();
						status.setDongleConnected(true);
						status.setVendor(m.group(1));
						status.setChip(m.group(2));
						status.setSerialNumber(m.group(3));
						break;
					}
				}
			}
		} catch (IOException e) {
			String error = "unable to read status";
			status = new RtlSdrStatus();
			status.setError(error);
			LOG.error(error, e);
		}
		return status;
	}

}
