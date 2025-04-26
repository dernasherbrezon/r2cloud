package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterStatus;

public class DeviceTransmitterFilter implements TransmitterFilter {

	private final TransmitterFilter framingFilter;
	private final DeviceConfiguration config;

	public DeviceTransmitterFilter(DeviceConfiguration config, TransmitterFilter framingFilter) {
		this.config = config;
		this.framingFilter = framingFilter;
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
		if (config.getMaximumSampleRate() != 0 && config.getMaximumSampleRate() < bandwidth) {
			return false;
		}
		boolean result = framingFilter.accept(transmitter);
		if (!result) {
			return false;
		}
		return true;
	}

}
