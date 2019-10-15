package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.ao73.Ao73;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;

public class Ao73Decoder extends TelemetryDecoder {

	public Ao73Decoder(Configuration config, Predict predict) {
		super(config, predict);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator bpsk = new BpskDemodulator(source, 1200, 1, 1500, 2000.0f, true);
		return new Ao73(bpsk);
	}

}
