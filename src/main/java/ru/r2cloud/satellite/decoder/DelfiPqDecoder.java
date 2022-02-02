package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.delfipq.DelfiPqBeacon;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class DelfiPqDecoder extends TelemetryDecoder {

	public DelfiPqDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		FskDemodulator demodulator = new FskDemodulator(source, baudRate, 600.0f, Util.convertDecimation(baudRate), 1000);
		return new Ax25G3ruhBeaconSource<>(demodulator, DelfiPqBeacon.class);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return DelfiPqBeacon.class;
	}

}
