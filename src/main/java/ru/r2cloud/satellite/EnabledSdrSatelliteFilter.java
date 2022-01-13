package ru.r2cloud.satellite;

import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;

public class EnabledSdrSatelliteFilter implements SatelliteFilter {

	public static final EnabledSdrSatelliteFilter INSTANCE = new EnabledSdrSatelliteFilter();

	@Override
	public boolean accept(Satellite satellite) {
		if (!satellite.isEnabled()) {
			return false;
		}
		if (satellite.getModulation() == null || !satellite.getModulation().equals(Modulation.LORA)) {
			return true;
		}
		return false;
	}
	
	@Override
	public String getName() {
		return "sdr";
	}
}
