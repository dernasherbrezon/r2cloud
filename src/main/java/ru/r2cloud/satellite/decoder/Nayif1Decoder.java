package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.nayif1.Nayif1;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Nayif1Decoder extends TelemetryDecoder {

	public Nayif1Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator bpsk = new BpskDemodulator(source, 1200, 5, 0.0, 2000.0f, true);
		return new Nayif1(bpsk);
	}
}
