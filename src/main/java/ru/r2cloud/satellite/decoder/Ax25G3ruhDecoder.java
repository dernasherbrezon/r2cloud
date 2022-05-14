package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Ax25G3ruhDecoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;
	private final byte[] assistedHeader;

	public Ax25G3ruhDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, byte[] assistedHeader) {
		super(predict, config);
		this.beacon = beacon;
		this.assistedHeader = assistedHeader;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new Ax25G3ruhBeaconSource<>(demodulator, beacon, true, assistedHeader);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
