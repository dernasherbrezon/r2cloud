package ru.r2cloud.sdr;

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

import ru.r2cloud.Lifecycle;
import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.MetricFormat;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.PpmType;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class SdrStatusDao implements Lifecycle, ConfigListener {

	private static final String PPM_CURRENT_PROPERTY_NAME = "ppm.current";
	private static final Logger LOG = LoggerFactory.getLogger(SdrStatusDao.class);

	private final Configuration config;
	private final SdrLock lock;
	private final ThreadPoolFactory threadpoolFactory;
	private final Metrics metrics;

	private ScheduledExecutorService executor;
	private SdrStatus status;

	private SdrStatusProcess statusProcess;
	private PpmProcess ppmProcess;
	private Integer currentPpm;
	private PpmType previousType;
	private ScheduledFuture<?> ppmTask;

	public SdrStatusDao(Configuration config, SdrLock lock, ThreadPoolFactory threadpoolFactory, Metrics metrics, ProcessFactory processFactory) {
		this.config = config;
		this.config.subscribe(this, "ppm.calculate.type", PPM_CURRENT_PROPERTY_NAME);
		this.lock = lock;
		this.threadpoolFactory = threadpoolFactory;
		this.metrics = metrics;
		switch (config.getSdrType()) {
		case RTLSDR:
			statusProcess = new RtlStatusProcess(config, processFactory);
			ppmProcess = new PpmProcess(config, processFactory);
			break;
		case PLUTOSDR:
			statusProcess = new PlutoStatusProcess(config, processFactory);
			break;
		case SDRSERVER:
			statusProcess = new SdrStatusProcess() {

				@Override
				public void terminate(long timeout) {
					// do nothing
				}

				@Override
				public SdrStatus getStatus() {
					// sdr-server doesn't support health checks yet
					SdrStatus result = new SdrStatus();
					result.setDongleConnected(true);
					return result;
				}
			};
			break;
		default:
			throw new IllegalArgumentException("unsupported sdr type: " + config.getSdrType());
		}
	}

	@Override
	public void onConfigUpdated() {
		currentPpm = config.getInteger(PPM_CURRENT_PROPERTY_NAME);
		PpmType type = config.getPpmType();
		switch (type) {
		case MANUAL:
			// its ok to cancel multiple times
			stopAutoPpm();
			break;
		case AUTO:
			// ensure called only once
			if (previousType == null || previousType.equals(PpmType.MANUAL)) {
				scheduleAutoPpm();
			}
			break;
		default:
			LOG.error("unknown ppm type: {}", type);
		}
	}

	@Override
	public synchronized void start() {
		if (executor != null) {
			return;
		}
		currentPpm = config.getInteger(PPM_CURRENT_PROPERTY_NAME);
		executor = threadpoolFactory.newScheduledThreadPool(1, new NamingThreadFactory("rtlsdr-tester"));
		previousType = config.getPpmType();
		if (PpmType.AUTO.equals(previousType) && ppmProcess != null && config.getBoolean("ppm.calculate")) {
			scheduleAutoPpm();
		} else {
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
				SdrStatus curStatus = statusProcess.getStatus();
				if (curStatus == null) {
					return ResultUtil.unknown();
				}
				synchronized (SdrStatusDao.this) {
					status = curStatus;
					if (status.isDongleConnected()) {
						return ResultUtil.healthy();
					} else {
						return ResultUtil.unhealthy(status.getError());
					}
				}
			}
		});
		metrics.getRegistry().gauge("ppm", new MetricSupplier<>() {

			@Override
			public Gauge<Integer> newMetric() {
				return new FormattedGauge<>(MetricFormat.NORMAL) {

					@Override
					public Integer getValue() {
						// graph will be displayed anyway.
						// fill it with 0
						synchronized (SdrStatusDao.this) {
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
					if (!lock.tryLock(SdrStatusDao.this)) {
						LOG.info("unable to lock for ppm calculation");
						return;
					}
					Integer ppm = null;
					try {
						ppm = ppmProcess.getPpm();
					} finally {
						lock.unlock(SdrStatusDao.this);
					}
					if (ppm == null) {
						return;
					}
					synchronized (SdrStatusDao.this) {
						currentPpm = ppm;
					}
					config.setProperty(PPM_CURRENT_PROPERTY_NAME, ppm);
					config.update();
				}
			}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			LOG.info("invalid time. ppm will be disabled", e);
		}
	}

	@Override
	public synchronized void stop() {
		if (ppmProcess != null) {
			ppmProcess.terminate(1000);
		}
		statusProcess.terminate(1000);
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
		LOG.info("stopped");
	}

}
