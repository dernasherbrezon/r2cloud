package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.ao73.Ao73;
import ru.r2cloud.jradio.ao73.Ao73Beacon;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Ao73Decoder extends TelemetryDecoder {

	public Ao73Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		Integer baudRate = req.getBaudRates().get(0);
		BpskDemodulator bpsk = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), 0.0, true);
		return new Ao73(bpsk);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Ao73Beacon.class;
	}
}
