package ru.r2cloud.predict;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationExtremumDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

public class MaxElevationHandler implements EventHandler<ElevationExtremumDetector> {

	private AbsoluteDate date;
	private final double maxElevation;

	public MaxElevationHandler(double maxElevation) {
		this.maxElevation = maxElevation;
	}

	@Override
	public Action eventOccurred(SpacecraftState s, ElevationExtremumDetector detector, boolean increasing) {
		if (FastMath.toDegrees(detector.getElevation(s)) > maxElevation) {
			date = s.getDate();
			return Action.STOP;
		}
		return Action.CONTINUE;
	}

	public AbsoluteDate getDate() {
		return date;
	}

}
