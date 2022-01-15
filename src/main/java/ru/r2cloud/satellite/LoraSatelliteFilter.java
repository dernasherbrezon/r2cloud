package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;

public class LoraSatelliteFilter extends DeviceSatelliteFilter {

	public LoraSatelliteFilter(DeviceConfiguration config) {
		super(config);
	}

	@Override
	public boolean accept(Satellite satellite) {
		if (!super.accept(satellite)) {
			return false;
		}
		if (satellite.getModulation() != null && satellite.getModulation().equals(Modulation.LORA)) {
			return true;
		}
		return false;
	}

}
