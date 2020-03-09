package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.smogp.SmogP;
import ru.r2cloud.jradio.smogp.SmogPBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class SmogPDecoder extends TelemetryDecoder {

	public SmogPDecoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 1250, req.getBandwidth(), 0.175f * 3);
		return new SmogP(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return SmogPBeacon.class;
	}

}
