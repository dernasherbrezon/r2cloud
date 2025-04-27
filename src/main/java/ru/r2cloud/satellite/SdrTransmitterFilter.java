package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Transmitter;

public class SdrTransmitterFilter extends DeviceTransmitterFilter {

	public SdrTransmitterFilter(DeviceConfiguration config, TransmitterFilter framingFilter) {
		super(config, framingFilter);
	}

	@Override
	public boolean accept(Transmitter satellite) {
		if (!super.accept(satellite)) {
			return false;
		}
		if (satellite.getModulation() == null || !satellite.getModulation().equals(Modulation.LORA)) {
			return true;
		}
		return false;
	}

}
