package ru.r2cloud.satellite;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Transmitter;

public class SdrServerTransmitterFilter extends SdrTransmitterFilter {

	public SdrServerTransmitterFilter(DeviceConfiguration config, FramingFilter framingFilter) {
		super(config, framingFilter);
	}

	@Override
	public boolean accept(Transmitter satellite) {
		if (!super.accept(satellite)) {
			return false;
		}
		if (satellite.getFraming().equals(Framing.APT)) {
			return false;
		}
		if (satellite.getFraming().equals(Framing.SATDUMP)) {
			return false;
		}
		return true;
	}

}
