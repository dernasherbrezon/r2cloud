package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.diy1.Diy1;
import ru.r2cloud.jradio.diy1.Diy1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Diy1Decoder extends TelemetryDecoder {

	public Diy1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		FskDemodulator demod = new FskDemodulator(source, 500, 5000.0f, 24, 2000, true);
		return new Diy1(demod);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Diy1Beacon.class;
	}

}
