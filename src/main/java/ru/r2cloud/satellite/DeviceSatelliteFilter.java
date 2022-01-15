package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Satellite;

public class DeviceSatelliteFilter implements SatelliteFilter {

	private final DeviceConfiguration config;

	public DeviceSatelliteFilter(DeviceConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean accept(Satellite satellite) {
		if (!satellite.isEnabled()) {
			return false;
		}
		long bandwidth = satellite.getBandwidth();
		if (bandwidth == 0) {
			bandwidth = satellite.getLoraBandwidth();
		}
		if ((satellite.getFrequency() + bandwidth / 2) > config.getMaximumFrequency()) {
			return false;
		}
		if ((satellite.getFrequency() - bandwidth / 2) < config.getMinimumFrequency()) {
			return false;
		}
		return true;
	}

}
