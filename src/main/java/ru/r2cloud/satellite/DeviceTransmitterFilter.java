package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterStatus;

public class DeviceTransmitterFilter implements TransmitterFilter {

	private final DeviceConfiguration config;

	public DeviceTransmitterFilter(DeviceConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean accept(Transmitter satellite) {
		if (!satellite.isEnabled()) {
			return false;
		}
		if (satellite.getStatus() == null) {
			return false;
		}
		if (satellite.getStatus().equals(TransmitterStatus.DECAYED) || satellite.getStatus().equals(TransmitterStatus.DISABLED)) {
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
