package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.mysat1.Mysat1;
import ru.r2cloud.jradio.mysat1.Mysat1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Mysat1Decoder extends TelemetryDecoder {

	public Mysat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator bpsk = new BpskDemodulator(source, 1200, 1, 0.0, 2000.0f, false);
		return new Mysat1(bpsk);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Mysat1Beacon.class;
	}
}
