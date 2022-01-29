package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.nayif1.Nayif1;
import ru.r2cloud.jradio.nayif1.Nayif1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Nayif1Decoder extends TelemetryDecoder {

	public Nayif1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		BpskDemodulator bpsk = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), 0, true);
		return new Nayif1(bpsk);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Nayif1Beacon.class;
	}
}
