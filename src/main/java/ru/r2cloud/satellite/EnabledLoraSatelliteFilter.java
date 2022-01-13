package ru.r2cloud.satellite;

import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;

public class EnabledLoraSatelliteFilter implements SatelliteFilter {

	public static final EnabledLoraSatelliteFilter INSTANCE = new EnabledLoraSatelliteFilter();

	@Override
	public boolean accept(Satellite satellite) {
		if (!satellite.isEnabled()) {
			return false;
		}
		if (satellite.getModulation() != null && satellite.getModulation().equals(Modulation.LORA)) {
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "lora";
	}
}
