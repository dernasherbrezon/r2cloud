package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.sstk1.StratosatTk1;
import ru.r2cloud.jradio.sstk1.StratosatTk1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class StratosatTk1Decoder extends TelemetryDecoder {

	public StratosatTk1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new StratosatTk1(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return StratosatTk1Beacon.class;
	}
}
