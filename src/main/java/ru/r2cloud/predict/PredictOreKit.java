package ru.r2cloud.predict;

import java.io.File;
import java.util.Date;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import ru.r2cloud.model.SatPass;
import ru.r2cloud.util.Configuration;

public class PredictOreKit {

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
		
		File orekitData = new File("/Users/dernasherbrezon/Downloads/orekit-data-master");
		DataProvidersManager manager = DataProvidersManager.getInstance();
		manager.addProvider(new DirectoryCrawler(orekitData));
		
		earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
		earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
		// FIXME download data
	}

	public static Long getDownlinkFreq(final Long freq, final long utcTimeMillis, final TopocentricFrame currentLocation, final TLEPropagator tlePropagator) {
		tlePropagator.setEphemerisMode();
		SpacecraftState currentState = tlePropagator.propagate(new AbsoluteDate(new Date(utcTimeMillis), TimeScalesFactory.getUTC()));
		final double rangeRate = currentLocation.getRangeRate(currentState.getPVCoordinates(), currentState.getFrame(), currentState.getDate());
		return (long) ((double) freq * (SPEED_OF_LIGHT - rangeRate * 1000.0) / SPEED_OF_LIGHT);
	}

	public SatPass calculateNext(Date current, TLEPropagator tlePropagator) {
		TopocentricFrame baseStationFrame = getPosition();
		if (baseStationFrame == null) {
			return null;
		}
		AbsoluteDate initialDate = new AbsoluteDate(current, TimeScalesFactory.getUTC());

		MaxElevationHandler maxElevationHandler = new MaxElevationHandler(guaranteedElevation);
		ElevationExtremumDetector maxDetector = new ElevationExtremumDetector(60, 0.001, baseStationFrame).withMaxIter(48 * 60).withHandler(maxElevationHandler);
		tlePropagator.clearEventsDetectors();
		tlePropagator.addEventDetector(new EventSlopeFilter<EventDetector>(maxDetector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
		tlePropagator.setSlaveMode();
		tlePropagator.propagate(new AbsoluteDate(initialDate, 3600. * 24 * 2));
		if (maxElevationHandler.getDate() == null) {
			return null;
		}

		MinElevationHandler minElevationHandler = new MinElevationHandler();
		ElevationDetector sta1Visi2 = new ElevationDetector(60, 0.001, baseStationFrame).withConstantElevation(FastMath.toRadians(minElevation)).withHandler(minElevationHandler);
		tlePropagator.clearEventsDetectors();
		tlePropagator.addEventDetector(sta1Visi2);
		// 20 mins before and 20 mins later
		AbsoluteDate startDate = maxElevationHandler.getDate().shiftedBy(-20 * 60.0);
		tlePropagator.propagate(startDate, startDate.shiftedBy(40 * 60.));

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
		// FIXME cache ground station posisiotn object and reload when config changes
		Double lat = config.getDouble("locaiton.lat");
		Double lon = config.getDouble("locaiton.lon");
		if (lat == null || lon == null) {
			return null;
		}
		GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0);
		TopocentricFrame baseStationFrame = new TopocentricFrame(earth, point, "station1");
		return baseStationFrame;
	}

}
