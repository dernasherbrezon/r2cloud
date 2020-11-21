package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.spooqy1.Spooqy1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Spooqy1Decoder extends TelemetryDecoder {

	public Spooqy1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		FskDemodulator demod = new FskDemodulator(source, 4800);
		return new Ax100BeaconSource<>(demod, 512, Spooqy1Beacon.class);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Spooqy1Beacon.class;
	}
}
