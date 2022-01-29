package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.salsat.Salsat;
import ru.r2cloud.jradio.snet.SnetBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SalsatDecoder extends TelemetryDecoder {

	public SalsatDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		AfskDemodulator demod = new AfskDemodulator(source, baudRate, -600, 1500, Util.convertDecimation(baudRate));
		return new Salsat(demod);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return SnetBeacon.class;
	}

}
