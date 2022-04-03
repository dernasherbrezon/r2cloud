package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.ao73.Ao73;
import ru.r2cloud.jradio.ao73.Ao73Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Ao73Decoder extends TelemetryDecoder {

	public Ao73Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new Ao73(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Ao73Beacon.class;
	}
}
