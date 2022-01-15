package ru.r2cloud.predict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.ElevationExtremumDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventSlopeFilter;
import org.orekit.propagation.events.FilterType;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.rotctrld.Position;
import ru.r2cloud.util.Configuration;

public class PredictOreKit {

	public static final double PREDICT_INTERVAL = 3600. * 24 * 2;
	private static final Logger LOG = LoggerFactory.getLogger(PredictOreKit.class);
	private static final double SPEED_OF_LIGHT = 2.99792458E8;

	private final double minElevation;
	private final double guaranteedElevation;
	private final Configuration config;
	private final Frame earthFrame;
	private final BodyShape earth;

	public PredictOreKit(Configuration config) {
		this.minElevation = config.getDouble("scheduler.elevation.min");
		this.guaranteedElevation = config.getDouble("scheduler.elevation.guaranteed");
		this.config = config;

		File orekitData = new File(config.getProperty("scheduler.orekit.path"));
		if (!orekitData.exists()) {
			LOG.info("orekit master data doesn't exist. downloading now. it might take some time");
			OreKitDataClient client = new OreKitDataClient(config.getProperties("scheduler.orekit.urls"));
			try {
				client.downloadAndSaveTo(orekitData.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
		manager.addProvider(new DirectoryCrawler(orekitData));

		earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
		earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
	}

	public Long getDownlinkFreq(final Long freq, final long utcTimeMillis, TopocentricFrame currentLocation, final TLEPropagator tlePropagator) {
		AbsoluteDate date = new AbsoluteDate(new Date(utcTimeMillis), TimeScalesFactory.getUTC());
		PVCoordinates currentState = tlePropagator.getPVCoordinates(date);
		final double rangeRate = currentLocation.getRangeRate(currentState, tlePropagator.getFrame(), date);
		return (long) ((double) freq * (SPEED_OF_LIGHT - rangeRate) / SPEED_OF_LIGHT);
	}

	public Position getSatellitePosition(long utcTimeMillis, TopocentricFrame currentLocation, final TLEPropagator tlePropagator) {
		AbsoluteDate date = new AbsoluteDate(new Date(utcTimeMillis), TimeScalesFactory.getUTC());
		PVCoordinates currentState = tlePropagator.getPVCoordinates(date);
		double azimuth = FastMath.toDegrees(currentLocation.getAzimuth(currentState.getPosition(), tlePropagator.getFrame(), date));
		double elevation = FastMath.toDegrees(currentLocation.getElevation(currentState.getPosition(), tlePropagator.getFrame(), date));
		Position result = new Position();
		result.setAzimuth(azimuth);
		result.setElevation(elevation);
		return result;
	}

	public List<SatPass> calculateSchedule(Date current, TLEPropagator tlePropagator) {
		List<SatPass> result = new ArrayList<>();
		TopocentricFrame baseStationFrame = getPosition();
		if (baseStationFrame == null) {
			return Collections.emptyList();
		}
		AbsoluteDate initialDate = new AbsoluteDate(current, TimeScalesFactory.getUTC());
		List<AbsoluteDate> max = new ArrayList<>();
		ElevationExtremumDetector maxDetector = new ElevationExtremumDetector(600, 1, baseStationFrame).withMaxIter(48 * 60).withHandler(new EventHandler<ElevationExtremumDetector>() {
			@Override
			public Action eventOccurred(SpacecraftState s, ElevationExtremumDetector detector, boolean increasing) {
				if (FastMath.toDegrees(detector.getElevation(s)) > guaranteedElevation) {
					max.add(s.getDate());
				}
				return Action.CONTINUE;
			}
		});
		tlePropagator.clearEventsDetectors();
		tlePropagator.addEventDetector(new EventSlopeFilter<EventDetector>(maxDetector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
		tlePropagator.setSlaveMode();
		tlePropagator.propagate(initialDate, new AbsoluteDate(initialDate, PREDICT_INTERVAL));
		for (AbsoluteDate curMax : max) {
			SatPass cur = findStartEnd(tlePropagator, baseStationFrame, curMax);
			if (cur != null) {
				if (cur.getStartMillis() < current.getTime()) {
					cur.setStart(initialDate);
				}
				result.add(cur);
			}
		}

		return result;
	}

	public SatPass calculateNext(Date current, TLEPropagator tlePropagator) {
		TopocentricFrame baseStationFrame = getPosition();
		if (baseStationFrame == null) {
			return null;
		}
		AbsoluteDate initialDate = new AbsoluteDate(current, TimeScalesFactory.getUTC());

		MaxElevationHandler maxElevationHandler = new MaxElevationHandler(guaranteedElevation);
		ElevationExtremumDetector maxDetector = new ElevationExtremumDetector(600, 1, baseStationFrame).withMaxIter(48 * 60).withHandler(maxElevationHandler);
		tlePropagator.clearEventsDetectors();
		tlePropagator.addEventDetector(new EventSlopeFilter<EventDetector>(maxDetector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
		tlePropagator.setSlaveMode();
		tlePropagator.propagate(initialDate, new AbsoluteDate(initialDate, PREDICT_INTERVAL));
		if (maxElevationHandler.getDate() == null) {
			return null;
		}

		return findStartEnd(tlePropagator, baseStationFrame, maxElevationHandler.getDate());
	}

	private SatPass findStartEnd(TLEPropagator tlePropagator, TopocentricFrame baseStationFrame, AbsoluteDate maxElevationTime) {
		MinElevationHandler minElevationHandler = new MinElevationHandler();
		ElevationDetector boundsDetector = new ElevationDetector(600, 1, baseStationFrame).withConstantElevation(FastMath.toRadians(minElevation)).withHandler(minElevationHandler);
		tlePropagator.clearEventsDetectors();
		tlePropagator.addEventDetector(boundsDetector);
		// 20 mins before and 20 mins later
		AbsoluteDate startDate = maxElevationTime.shiftedBy(-20 * 60.0);
		tlePropagator.propagate(startDate, startDate.shiftedBy(40 * 60.0));

		if (minElevationHandler.getStart() == null || minElevationHandler.getEnd() == null) {
			return null;
		}

		SatPass result = new SatPass();
		result.setStart(minElevationHandler.getStart());
		result.setEnd(minElevationHandler.getEnd());
		return result;
	}

	public TopocentricFrame getPosition() {
		// get the current position
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		if (lat == null || lon == null) {
			return null;
		}
		return getPosition(new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0));
	}

	public TopocentricFrame getPosition(GeodeticPoint point) {
		return new TopocentricFrame(earth, point, "station1");
	}

}
