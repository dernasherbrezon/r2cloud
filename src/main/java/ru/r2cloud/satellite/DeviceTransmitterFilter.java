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
	public boolean accept(Transmitter transmitter) {
		if (transmitter.getStatus() == null) {
			return false;
		}
		if (transmitter.getStatus().equals(TransmitterStatus.DECAYED) || transmitter.getStatus().equals(TransmitterStatus.DISABLED)) {
			return false;
		}
		long bandwidth = transmitter.getBandwidth();
		if (bandwidth == 0) {
			bandwidth = transmitter.getLoraBandwidth();
		}
		if ((transmitter.getFrequency() + bandwidth / 2) > config.getMaximumFrequency()) {
			return false;
		}
		if ((transmitter.getFrequency() - bandwidth / 2) < config.getMinimumFrequency()) {
			return false;
		}
		return true;
	}

}
