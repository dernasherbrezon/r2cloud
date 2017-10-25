package ru.r2cloud.tle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.ThreadPoolFactory;
import ru.r2cloud.util.Util;

public class TLEReloader implements ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(TLEReloader.class);
	private final ThreadPoolFactory threadFactory;
	private final TLEDao dao;
	private final Configuration config;
	private final Clock clock;

	private ScheduledExecutorService executor = null;

	public TLEReloader(Configuration config, TLEDao dao, ThreadPoolFactory threadFactory, Clock clock) {
		this.config = config;
		this.threadFactory = threadFactory;
		this.clock = clock;
		this.dao = dao;
		this.config.subscribe(this, "satellites.enabled");
	}

	@Override
	public synchronized void onConfigUpdated() {
		boolean enabled = config.getBoolean("satellites.enabled");
		if (executor == null && enabled) {
			start();
		} else if (executor != null && !enabled) {
			stop();
		}
	}

	public synchronized void start() {
		if( !config.getBoolean("satellites.enabled") ) {
			LOG.info("tle tracking is disabled");
			return;
		}
		if (executor != null) {
			return;
		}
		executor = threadFactory.newScheduledThreadPool(1, new NamingThreadFactory("tle-updater"));
		SimpleDateFormat sdf = new SimpleDateFormat("u HH:mm");
		try {
			long current = clock.millis();
			Date date = sdf.parse(config.getProperty("tle.update.timeUTC"));
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			Calendar executeAt = Calendar.getInstance();
			executeAt.setTimeInMillis(current);
			executeAt.set(Calendar.DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK));
			executeAt.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
			executeAt.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
			executeAt.set(Calendar.SECOND, 0);
			executeAt.set(Calendar.MILLISECOND, 0);
			if (executeAt.getTimeInMillis() < current) {
				executeAt.add(Calendar.WEEK_OF_YEAR, 1);
			}
			LOG.info("next tle update at: " + executeAt.getTime());
			executor.scheduleAtFixedRate(new SafeRunnable() {

				@Override
				public void doRun() {
					dao.reload();
				}
			}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			LOG.info("invalid time. tle will be disabled", e);
		}
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}

}
