package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.MetricFormat;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.RtlSdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.health.HealthCheck;

public class RtlSdrStatusDao implements Lifecycle {

	private final static Logger LOG = LoggerFactory.getLogger(RtlSdrStatusDao.class);
	private final static Pattern DEVICEPATTERN = Pattern.compile("^  0:  (.*?), (.*?), SN: (.*?)$");
	private final static Pattern PPMPATTERN = Pattern.compile("real sample rate: \\d+ current PPM: \\d+ cumulative PPM: (\\d+)");

	private final Configuration config;
	private final RtlSdrLock lock;

	private ScheduledExecutorService executor = null;
	private RtlSdrStatus status = null;
	private String rtltestError = null;

	private volatile Integer currentPpm;

	public RtlSdrStatusDao(Configuration config, RtlSdrLock lock) {
		this.config = config;
		this.lock = lock;
		this.currentPpm = config.getInteger("ppm.current");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized void start() {
		if (executor != null) {
			return;
		}
		executor = Executors.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		executor.scheduleAtFixedRate(new SafeRunnable() {

			@Override
			public void doRun() {
				reload();
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
				LOG.info("next ppm execution at: " + executeAt.getTime());
				executor.scheduleAtFixedRate(new SafeRunnable() {

					@Override
					public void doRun() {
						reloadPpm();
					}
				}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);

			} catch (ParseException e) {
				LOG.info("invalid time. ppm will be disabled", e);
			}
		}
		Metrics.HEALTH_REGISTRY.register("rtltest", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				if (rtltestError == null) {
					return ResultUtil.healthy();
				} else {
					return ResultUtil.unhealthy(rtltestError);
				}
			}
		});
		Metrics.HEALTH_REGISTRY.register("rtldongle", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				if (status != null) {
					return ResultUtil.healthy();
				} else {
					return ResultUtil.unhealthy("No supported devices found");
				}
			}
		});
		Metrics.REGISTRY.gauge("ppm", new MetricSupplier<Gauge>() {

			@Override
			public Gauge<?> newMetric() {
				return new FormattedGauge<Integer>(MetricFormat.NORMAL) {

					@Override
					public Integer getValue() {
						// graph will be displayed anyway.
						// fill it with 0
						if (currentPpm == null) {
							return 0;
						}
						return currentPpm;
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

	private void reloadPpm() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("ppm calculation");
		}
		if (!lock.tryLock(this)) {
			LOG.info("unable to lock for ppm calculation");
			return;
		}

		Process rtlTest = null;
		try {
			rtlTest = new ProcessBuilder().command(new String[] { config.getProperty("stdbuf.path"), "-i0", "-o0", "-e0", config.getProperty("rtltest.path"), "-p2" }).redirectErrorStream(true).start();
			BufferedReader r = new BufferedReader(new InputStreamReader(rtlTest.getInputStream()));
			String curLine = null;
			int numberOfSamples = 0;
			while ((curLine = r.readLine()) != null) {
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
							currentPpm = Integer.valueOf(ppmStr);
							config.setProperty("ppm.current", String.valueOf(currentPpm));
							config.update();
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			LOG.error("unable to calculate ppm", e);
		} finally {
			Util.shutdownProcess(rtlTest, 5000);
			lock.unlock(this);
		}
	}

	private void reload() {
		if (!lock.tryLock(this)) {
			LOG.info("unable to lock rtl_test");
			return;
		}
		try {
			Process rtlTest = new ProcessBuilder().command(new String[] { config.getProperty("rtltest.path"), "-t" }).start();
			BufferedReader r = new BufferedReader(new InputStreamReader(rtlTest.getErrorStream()));
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				if (curLine.startsWith("No supported")) {
					status = null;
					return;
				} else {
					Matcher m = DEVICEPATTERN.matcher(curLine);
					if (m.find()) {
						status = new RtlSdrStatus();
						status.setVendor(m.group(1));
						status.setChip(m.group(2));
						status.setSerialNumber(m.group(3));
						break;
					}
				}
			}
			rtltestError = null;
		} catch (IOException e) {
			rtltestError = "unable to read status";
			LOG.error(rtltestError, e);
		} finally {
			lock.unlock(this);
		}
	}

}
