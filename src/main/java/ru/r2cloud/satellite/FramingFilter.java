package ru.r2cloud.satellite;

import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;

public class FramingFilter implements TransmitterFilter {

	private final boolean satdumpAvailable;
	private final boolean wxtoimgAvailable;
	private final boolean validateExternal;

	public FramingFilter(Configuration config) {
		this.satdumpAvailable = config.getBoolean("satellites.satdump.available");
		this.wxtoimgAvailable = config.getBoolean("satellites.wxtoimg.available");
		this.validateExternal = config.getBoolean("satellites.validate.external");
	}

	@Override
	public boolean accept(Transmitter transmitter) {
		if (transmitter.getFraming() == null) {
			return true;
		}
		if (!validateExternal) {
			return true;
		}
		if (transmitter.getFraming().equals(Framing.SATDUMP)) {
			return satdumpAvailable;
		}
		if (transmitter.getFraming().equals(Framing.APT)) {
			return wxtoimgAvailable;
		}
		return true;
	}

}
