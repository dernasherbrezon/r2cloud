package ru.r2cloud.tle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;

public class TLEDao {

	private static final Logger LOG = LoggerFactory.getLogger(TLEDao.class);

	private final Configuration config;
	private final SatelliteDao satelliteDao;

	private ScheduledExecutorService executor = null;

	public TLEDao(Configuration config, SatelliteDao satelliteDao) {
		this.config = config;
		this.satelliteDao = satelliteDao;
	}

	public void start() {
		executor = Executors.newScheduledThreadPool(1, new NamingThreadFactory("tle-updater"));
		SimpleDateFormat sdf = new SimpleDateFormat("u HH:mm");
		try {
			Date date = sdf.parse(config.getProperty("tle.update.timeUTC"));
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			Calendar executeAt = Calendar.getInstance();
			executeAt.set(Calendar.DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK));
			executeAt.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
			executeAt.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
			executeAt.set(Calendar.SECOND, 0);
			executeAt.set(Calendar.MILLISECOND, 0);
			long current = System.currentTimeMillis();
			if (executeAt.getTimeInMillis() < current) {
				executeAt.add(Calendar.WEEK_OF_YEAR, 1);
			}
			LOG.info("next ppm execution at: " + executeAt.getTime());
			executor.scheduleAtFixedRate(new SafeRunnable() {

				@Override
				public void doRun() {
					reload();
				}
			}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			LOG.info("invalid time. ppm will be disabled", e);
		}
	}

	private void reload() {
		
	}
	
	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}
}
