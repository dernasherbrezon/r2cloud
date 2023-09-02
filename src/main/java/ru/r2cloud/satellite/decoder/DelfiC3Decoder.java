package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.delfic3.DelfiC3;
import ru.r2cloud.jradio.delfic3.DelfiC3Beacon;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class DelfiC3Decoder extends TelemetryDecoder {

	public DelfiC3Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new DelfiC3(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return DelfiC3Beacon.class;
	}

}
