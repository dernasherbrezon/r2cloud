package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.RtlSdrStatus;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.uitl.NamingThreadFactory;
import ru.r2cloud.uitl.ResultUtil;
import ru.r2cloud.uitl.SafeRunnable;

import com.codahale.metrics.health.HealthCheck;

public class RtlSdrStatusDao {

	private final static Logger LOG = Logger.getLogger(RtlSdrStatusDao.class.getName());
	private final static Pattern DEVICEPATTERN = Pattern.compile("^  0:  (.*?), (.*?), SN: (.*?)$");

	private final Configuration props;

	private ScheduledExecutorService executor = null;
	private RtlSdrStatus status = null;
	private String rtltestError = null;

	public RtlSdrStatusDao(Configuration config) {
		this.props = config;
	}

	public void start() {
		executor = Executors.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		executor.scheduleAtFixedRate(new SafeRunnable() {

			@Override
			public void doRun() {
				reload();
			}
		}, 0, props.getLong("rtltest.interval.seconds"), TimeUnit.SECONDS);
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
	}

	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	private void reload() {
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
					}
				}
			}
			rtltestError = null;
		} catch (IOException e) {
			rtltestError = "unable to read status";
			LOG.log(Level.SEVERE, rtltestError, e);
		}
	}

}
