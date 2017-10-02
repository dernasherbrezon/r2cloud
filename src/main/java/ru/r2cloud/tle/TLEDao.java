package ru.r2cloud.tle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;

public class TLEDao implements ConfigListener {

	private static final Logger LOG = LoggerFactory.getLogger(TLEDao.class);

	private final Configuration config;
	private final SatelliteDao satelliteDao;
	private final File basepath;

	private ScheduledExecutorService executor = null;

	public TLEDao(Configuration config, SatelliteDao satelliteDao) {
		this.config = config;
		this.config.subscribe(this);
		this.satelliteDao = satelliteDao;
		this.basepath = Util.initDirectory(config.getProperty("satellites.basepath.location"));
	}

	@Override
	public void onConfigUpdated() {
		boolean enabled = config.getBoolean("satellites.enabled");
		if (executor == null && enabled) {
			start();
		} else if (executor != null && !enabled) {
			stop();
		}
	}

	public synchronized void start() {
		if (!config.getBoolean("satellites.enabled")) {
			LOG.info("tle tracking is disabled");
			return;
		}
		if (executor != null) {
			return;
		}
		//TODO check the last tle update time from config
		//if more than 1 week, than update tle now
		for (Satellite cur : satelliteDao.findSupported()) {
			File tle = new File(basepath, cur.getId() + File.separator + "tle.txt");
			if (!tle.exists()) {
				LOG.info("missing tle for " + cur.getName() + ". reloading all tle");
				reload();
				break;
			}
			try (BufferedReader r = new BufferedReader(new FileReader(tle))) {
				String line1 = r.readLine();
				if (line1 == null) {
					continue;
				}
				String line2 = r.readLine();
				if (line2 == null) {
					continue;
				}
				cur.setTleLine1(line1);
				cur.setTleLine2(line2);
			} catch (IOException e) {
				LOG.error("unable to load TLE for " + cur.getId(), e);
				reload();
				break;
			}
		}
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
			LOG.info("next tle update at: " + executeAt.getTime());
			executor.scheduleAtFixedRate(new SafeRunnable() {

				@Override
				public void doRun() {
					reload();
				}
			}, executeAt.getTimeInMillis() - current, TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			LOG.info("invalid time. tle will be disabled", e);
		}
	}

	private void reload() {
		HttpURLConnection con = null;
		try {
			URL obj = new URL("http://celestrak.com/NORAD/elements/weather.txt");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to get weather tle. response code: " + responseCode + ". See logs for details");
				Util.toLog(LOG, con.getInputStream());
			} else {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					// only first line matters
					String curLine = null;
					while ((curLine = in.readLine()) != null) {
						Satellite satellite = satelliteDao.findByName(curLine.trim());
						String line1 = in.readLine();
						if (line1 == null) {
							break;
						}
						String line2 = in.readLine();
						if (line2 == null) {
							break;
						}
						if (satellite == null) {
							continue;
						}
						File output = new File(basepath, satellite.getId() + File.separator + "tle.txt");
						if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
							LOG.error("unable to create directory for satellite: " + satellite.getName());
							continue;
						}
						satellite.setTleLine1(line1);
						satellite.setTleLine2(line2);
						try (BufferedWriter w = new BufferedWriter(new FileWriter(output))) {
							w.append(line1);
							w.newLine();
							w.append(line2);
							w.newLine();
						}
					}
				}
				config.setProperty("satellites.tle.lastupdateAtMillis", String.valueOf(System.currentTimeMillis()));
				config.update();
			}
		} catch (Exception e) {
			LOG.error("unable to get weather tle", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	public synchronized void stop() {
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
	}
}
