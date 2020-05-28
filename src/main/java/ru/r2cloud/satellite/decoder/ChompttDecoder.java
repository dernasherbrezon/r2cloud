package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.chomptt.Chomptt;
import ru.r2cloud.jradio.chomptt.ChompttBeacon;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class ChompttDecoder extends TelemetryDecoder {

	public ChompttDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		AfskDemodulator demod = new AfskDemodulator(source, 1200, 500, 1700, 0.175f * 3);
		return new Chomptt(demod);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return ChompttBeacon.class;
	}
}
