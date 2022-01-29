package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.alsat1n.Alsat1n;
import ru.r2cloud.jradio.alsat1n.Alsat1nBeacon;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Alsat1nDecoder extends TelemetryDecoder {

	public Alsat1nDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		FskDemodulator demod = new FskDemodulator(source, req.getBaudRates().get(0));
		return new Alsat1n(demod);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Alsat1nBeacon.class;
	}

}
