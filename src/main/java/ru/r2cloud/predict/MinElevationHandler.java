package ru.r2cloud.predict;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

public class MinElevationHandler implements EventHandler<ElevationDetector> {

	private AbsoluteDate start;
	private AbsoluteDate end;

	@Override
	public Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
		if (increasing) {
			start = s.getDate();
			return Action.CONTINUE;
		}
		end = s.getDate();
		return Action.STOP;
	}

	public AbsoluteDate getStart() {
		return start;
	}

	public AbsoluteDate getEnd() {
		return end;
	}

}
