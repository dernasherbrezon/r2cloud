package ru.r2cloud.satellite;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.util.ConfigListener;
import ru.r2cloud.util.Configuration;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;

public class Predict implements ConfigListener {

	private final double minElevation;
	private final double guaranteedElevation;
	private final Configuration config;

	private GroundStationPosition currentLocation;

	public Predict(Configuration config) {
		this.minElevation = config.getDouble("scheduler.elevation.min");
		this.guaranteedElevation = config.getDouble("scheduler.elevation.guaranteed");
		this.config = config;
		this.config.subscribe(this, "locaiton.lat", "locaiton.lon");
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		if (lat == null || lon == null) {
			this.currentLocation = null;
		} else {
			this.currentLocation = new GroundStationPosition(lat, lon, 0.0);
		}
	}

	@Override
	public void onConfigUpdated() {
		this.currentLocation = new GroundStationPosition(config.getDouble("locaiton.lat"), config.getDouble("locaiton.lon"), 0.0);
	}

	public SatPass calculateNext(Date current, Satellite satellite) {
		if (currentLocation == null || !satellite.willBeSeen(currentLocation)) {
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
						if (previous == null) {
							// in the middle of the pass
							start = position;
						} else {
							start = findPrecise(previous, position, satellite);
						}
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