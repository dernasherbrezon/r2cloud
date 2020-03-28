package ru.r2cloud.satellite;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.util.Configuration;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;

public class Predict {

	private static final double SPEED_OF_LIGHT = 2.99792458E8;

	private final double minElevation;
	private final double guaranteedElevation;
	private final Configuration config;

	public Predict(Configuration config) {
		this.minElevation = config.getDouble("scheduler.elevation.min");
		this.guaranteedElevation = config.getDouble("scheduler.elevation.guaranteed");
		this.config = config;
	}

	public static Long getDownlinkFreq(final Long freq, final long utcTimeMillis, final GroundStationPosition currentLocation, final Satellite satellite) {
		final SatPos satPos = satellite.getPosition(currentLocation, new Date(utcTimeMillis));
		final double rangeRate = satPos.getRangeRate();
		return (long) ((double) freq * (SPEED_OF_LIGHT - rangeRate * 1000.0) / SPEED_OF_LIGHT);
	}

	public SatPass calculateNext(Date current, Satellite satellite) {
		GroundStationPosition currentLocation = getPosition();
		if (currentLocation == null) {
			return null;
		}
		if (!satellite.willBeSeen(currentLocation)) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(current);
		boolean matched = false;
		SatPos previous = null;
		SatPos start = null;
		double maxElevation = 0.0;
		for (int i = 0; i < 48; i++) {
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
							start = findPrecise(currentLocation, previous, position, satellite);
						}
					}
					matched = true;
				} else {
					if (matched && maxElevation >= guaranteedElevation) {
						SatPass result = new SatPass();
//						result.setStart(start);
//						result.setEnd(findPrecise(currentLocation, previous, position, satellite));
						return result;
					}
					matched = false;
				}
				previous = position;
			}
		}
		return null;
	}

	// log(n) binary search of visible pass. precision is 1 second
	private SatPos findPrecise(GroundStationPosition currentLocation, SatPos start, SatPos end, Satellite satellite) {
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
				return findPrecise(currentLocation, start, newEnd, satellite);
			} else {
				return findPrecise(currentLocation, newEnd, end, satellite);
			}
		} else {
			if (isMiddleVisible) {
				return findPrecise(currentLocation, newEnd, end, satellite);
			} else {
				return findPrecise(currentLocation, start, newEnd, satellite);
			}
		}
	}

	public GroundStationPosition getPosition() {
		// get the current position
		// FIXME cache ground station posisiotn object and reload when config changes
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		if (lat == null || lon == null) {
			return null;
		}
		return new GroundStationPosition(lat, lon, 0.0);
	}

	private static double elevation(SatPos sat) {
		return sat.getElevation() / (Math.PI * 2.0) * 360;
	}
}
