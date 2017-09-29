package ru.r2cloud;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Scheduler {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final SatelliteDao satellites;
	private final Configuration config;
	private final double minElevation;
	private final double guaranteedElevation;
	private final GroundStationPosition currentLocation;
	private ScheduledExecutorService executor = null;

	public Scheduler(Configuration config, SatelliteDao satellites) {
		this.config = config;
		this.satellites = satellites;
		this.minElevation = config.getDouble("scheduler.elevation.min");
		this.guaranteedElevation = config.getDouble("scheduler.elevation.guaranteed");
		// TODO doesnt support reloading coordinates
		this.currentLocation = new GroundStationPosition(config.getDouble("locaiton.lat"), config.getDouble("locaiton.lon"), 0.0);
	}

	public void start() {
		if (!config.getBoolean("satellites.enabled")) {
			LOG.info("satellite scheduler is disabled");
			return;
		}
		executor = Executors.newScheduledThreadPool(2, new NamingThreadFactory("scheduler"));
		for (ru.r2cloud.model.Satellite cur : satellites.findSupported()) {
			TLE tle = new TLE(new String[] { cur.getName(), cur.getTleLine1(), cur.getTleLine2() });
			Satellite satellite = SatelliteFactory.createSatellite(tle);
			schedule(cur, satellite);
		}
	}

	private void schedule(ru.r2cloud.model.Satellite cur, Satellite satellite) {
		long current = System.currentTimeMillis();
		SatPass nextPass = findNext(new Date(current), satellite);
		if (nextPass == null) {
			LOG.info("can't find next pass for " + cur.getName());
			return;
		}
		LOG.info("scheduled next pass for " + cur.getName() + ": " + nextPass);
		Future<?> future = executor.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				
				// TODO Auto-generated method stub

			}
		}, nextPass.getStart().getTime().getTime() - current, TimeUnit.MILLISECONDS);
		executor.schedule(new SafeRunnable() {

			@Override
			public void doRun() {
				future.cancel(true);
				// FIME send image schedule next pass
				
				schedule(cur, satellite);
			}
		}, nextPass.getEnd().getTime().getTime() - current, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	// package protected for tests
	SatPass findNext(Date current, Satellite satellite) {
		if (!satellite.willBeSeen(currentLocation)) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(current);
		boolean matched = false;
		SatPos previous = null;
		SatPos start = null;
		double maxElevation = 0.0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 60; j++) {
				cal.add(Calendar.MINUTE, 1);
				SatPos position = satellite.getPosition(currentLocation, cal.getTime());
				double elevation = elevation(position);
				if (elevation >= minElevation) {
					// calculate max elevation only during satellite pass
					maxElevation = Math.max(maxElevation, elevation);
					if (!matched) {
						start = findPrecise(previous, position, satellite);
					}
					matched = true;
				} else {
					if (matched) {
						if (maxElevation >= guaranteedElevation) {
							SatPass result = new SatPass();
							result.setStart(start);
							result.setEnd(findPrecise(previous, position, satellite));
							return result;
						}
					}
					matched = false;
				}
				previous = position;
			}
		}
		return null;
	}

	// log(n) binary search of visible pass. precision is 1 second
	private SatPos findPrecise(SatPos start, SatPos end, Satellite satellite) {
		long middle = (end.getTime().getTime() / TimeUnit.SECONDS.toMillis(1) - start.getTime().getTime() / TimeUnit.SECONDS.toMillis(1)) / 2;
		if (middle == 0) {
			if (elevation(end) >= minElevation) {
				return end;
			} else {
				return start;
			}
		}
		SatPos newEnd = satellite.getPosition(currentLocation, new Date(start.getTime().getTime() + middle * TimeUnit.SECONDS.toMillis(1)));
		boolean isMiddleVisible = elevation(newEnd) >= minElevation;
		// return left of right part of timeline
		if (elevation(end) >= minElevation) {
			if (isMiddleVisible) {
				return findPrecise(start, newEnd, satellite);
			} else {
				return findPrecise(newEnd, end, satellite);
			}
		} else {
			if (isMiddleVisible) {
				return findPrecise(newEnd, end, satellite);
			} else {
				return findPrecise(start, newEnd, satellite);
			}
		}
	}

	private static double elevation(SatPos sat) {
		return sat.getElevation() / (Math.PI * 2.0) * 360;
	}

}
