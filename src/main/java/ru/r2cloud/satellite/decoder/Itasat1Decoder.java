package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.itasat1.Itasat1;
import ru.r2cloud.jradio.itasat1.Itasat1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Itasat1Decoder extends TelemetryDecoder {

	public Itasat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator bpsk = new BpskDemodulator(source, 1200, 5, 200, false);
		SoftToHard s2h = new SoftToHard(bpsk);
		return new Itasat1(s2h);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Itasat1Beacon.class;
	}

}
