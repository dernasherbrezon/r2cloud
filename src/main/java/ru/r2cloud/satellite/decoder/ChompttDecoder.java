package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.chomptt.Chomptt;
import ru.r2cloud.jradio.chomptt.ChompttBeacon;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class ChompttDecoder extends TelemetryDecoder {

	public ChompttDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Chomptt(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return ChompttBeacon.class;
	}
}
