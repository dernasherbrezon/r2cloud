package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
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
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new Diy1(demodulator);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Diy1Beacon.class;
	}

}
