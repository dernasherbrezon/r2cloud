package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.delfipq.DelfiPqBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class DelfiPqDecoder extends TelemetryDecoder {

	public DelfiPqDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new Ax25G3ruhBeaconSource<>(demodulator, DelfiPqBeacon.class);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return DelfiPqBeacon.class;
	}

}
