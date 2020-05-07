package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.quetzal1.Quetzal1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Quetzal1Decoder extends TelemetryDecoder {

	public Quetzal1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 4800, req.getBandwidth(), 0.175f * 3, 0.02f);
		return new Ax25G3ruhBeaconSource<>(demodulator, Quetzal1Beacon.class);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Quetzal1Beacon.class;
	}
}
