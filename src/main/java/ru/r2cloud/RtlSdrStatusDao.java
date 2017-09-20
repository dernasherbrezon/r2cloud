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
import ru.r2cloud.rx.ADSB;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.SafeRunnable;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.health.HealthCheck;

public class RtlSdrStatusDao {

	private final static Logger LOG = LoggerFactory.getLogger(RtlSdrStatusDao.class);
	private final static Pattern DEVICEPATTERN = Pattern.compile("^  0:  (.*?), (.*?), SN: (.*?)$");
	private final static Pattern PPMPATTERN = Pattern.compile("real sample rate: \\d+ current PPM: \\d+ cumulative PPM: (\\d+)");

	private final Configuration props;
	private final ADSB adsb;

	private ScheduledExecutorService executor = null;
	private RtlSdrStatus status = null;
	private String rtltestError = null;
	private final long intervalSeconds;

	private boolean calculatePpm;
	private int calculatePpmHours;
	private int calculatePpmMinutes;
	private Integer currentPpm;

	public RtlSdrStatusDao(Configuration config, ADSB adsb) {
		this.props = config;
		this.adsb = adsb;
		this.calculatePpm = config.getBoolean("ppm.calculate");
		this.intervalSeconds = props.getLong("rtltest.interval.seconds");
		this.currentPpm = props.getInteger("ppm.current");
		if (calculatePpm) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			try {
				Date date = sdf.parse(config.getProperty("ppm.calculate.timeUTC"));
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				calculatePpmHours = cal.get(Calendar.HOUR_OF_DAY);
				calculatePpmMinutes = cal.get(Calendar.MINUTE);
			} catch (ParseException e) {
				LOG.info("invalid time. ppm will be disabled", e);
				calculatePpm = false;
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void start() {
		executor = Executors.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		executor.scheduleAtFixedRate(new SafeRunnable() {

			@Override
			public void doRun() {
				reload();
			}
		}, 0, intervalSeconds, TimeUnit.SECONDS);
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
						return currentPpm;
					}
				};
			}
		});
	}

	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	private void reload() {
		Calendar current = Calendar.getInstance();
		Calendar start = getPpmExecution();
		start.add(Calendar.SECOND, -(int) intervalSeconds / 2);
		Calendar end = getPpmExecution();
		end.add(Calendar.SECOND, (int) intervalSeconds / 2);
		if (current.after(start) && current.before(end)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("ppm calculation");
			}
			adsb.stop();

			Process rtlTest = null;
			try {
				rtlTest = new ProcessBuilder().command(new String[] { props.getProperty("rtltest.path"), "-p1" }).redirectErrorStream(true).start();
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
								props.setProperty("ppm.current", String.valueOf(currentPpm));
								props.update();
							}
						}
					}
				}
			} catch (IOException e) {
				LOG.error("unable to calculate ppm", e);
			} finally {
				if (rtlTest != null && rtlTest.isAlive()) {
					try {
						int statusCode = rtlTest.destroyForcibly().waitFor();
						if (statusCode != 0) {
							LOG.info("invalid status code while waiting for rtl_test to stop: " + statusCode);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			adsb.start();
		} else {
			try {
				Process rtlTest = new ProcessBuilder().command(new String[] { props.getProperty("rtltest.path"), "-t" }).start();
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
			}
		}
	}

	private Calendar getPpmExecution() {
		Calendar result = Calendar.getInstance();
		result.set(Calendar.HOUR_OF_DAY, calculatePpmHours);
		result.set(Calendar.MINUTE, calculatePpmMinutes);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		return result;
	}

}
