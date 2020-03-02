package ru.r2cloud;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.health.HealthCheck;

import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.MetricFormat;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.PpmType;
import ru.r2cloud.model.RtlSdrStatus;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class RtlSdrStatusDao implements Lifecycle, ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(RtlSdrStatusDao.class);

	private final Configuration config;
	private final RtlSdrLock lock;
	private final ThreadPoolFactory threadpoolFactory;
	private final Metrics metrics;

	private ScheduledExecutorService executor;
	private RtlSdrStatus status;

	private RtlStatusProcess statusProcess;
	private PpmProcess ppmProcess;
	private Integer currentPpm;
	private ScheduledFuture<?> ppmTask;

	public RtlSdrStatusDao(Configuration config, RtlSdrLock lock, ThreadPoolFactory threadpoolFactory, Metrics metrics, ProcessFactory processFactory) {
		this.config = config;
		this.config.subscribe(this, "ppm.calculate.type", "ppm.current");
		this.lock = lock;
		this.threadpoolFactory = threadpoolFactory;
		this.metrics = metrics;
		statusProcess = new RtlStatusProcess(config, processFactory);
		ppmProcess = new PpmProcess(config, processFactory);
	}

	@Override
	public void onConfigUpdated() {
		currentPpm = config.getInteger("ppm.current");
		PpmType type = config.getPpmType();
		switch (type) {
		case MANUAL:
			// its ok to cancel multiple times
			stopAutoPpm();
			break;
		case AUTO:
			// ensure called only once
			if (ppmTask == null) {
				scheduleAutoPpm();
			}
			break;
		default:
			LOG.error("unknown ppm type: " + type);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized void start() {
		if (executor != null) {
			return;
		}
		currentPpm = config.getInteger("ppm.current");
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		PpmType type = config.getPpmType();
		if (PpmType.AUTO.equals(type)) {
			if (config.getBoolean("ppm.calculate")) {
				scheduleAutoPpm();
			}
		} else if (PpmType.MANUAL.equals(type)) {
			LOG.info("ppm configured manually: {}", currentPpm);
		}
		metrics.getHealthRegistry().register("rtltest", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				return ResultUtil.healthy();
			}
		});
		metrics.getHealthRegistry().register("rtldongle", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				RtlSdrStatus curStatus = statusProcess.getStatus();
				if (curStatus == null) {
					return ResultUtil.unknown();
				}
				synchronized (RtlSdrStatusDao.this) {
					status = curStatus;
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

	private void stopAutoPpm() {
		if (ppmTask == null) {
			return;
		}
		ppmTask.cancel(true);
	}

	private void scheduleAutoPpm() {
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
			LOG.info("schedule ppm auto update. next execution at: {}", executeAt.getTime());
			ppmTask = executor.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					if (!lock.tryLock(RtlSdrStatusDao.this)) {
						LOG.info("unable to lock for ppm calculation");
						return;
					}
					Integer ppm = null;
					try {
						ppm = ppmProcess.getPpm();
					} finally {
						lock.unlock(RtlSdrStatusDao.this);
					}
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

	@Override
	public synchronized void stop() {
		ppmProcess.terminate(1000);
		statusProcess.terminate(1000);
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
		LOG.info("stopped");
	}

}
