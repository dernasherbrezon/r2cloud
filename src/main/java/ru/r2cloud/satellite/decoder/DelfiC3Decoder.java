package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.delfic3.DelfiC3;
import ru.r2cloud.jradio.delfic3.DelfiC3Beacon;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class DelfiC3Decoder extends TelemetryDecoder {

	public DelfiC3Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		BpskDemodulator bpsk = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), -2000, true);
		return new DelfiC3(bpsk);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return DelfiC3Beacon.class;
	}

}
