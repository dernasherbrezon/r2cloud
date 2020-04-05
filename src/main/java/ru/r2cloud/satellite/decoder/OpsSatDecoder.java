package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.opssat.OpsSat;
import ru.r2cloud.jradio.opssat.OpsSatBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class OpsSatDecoder extends TelemetryDecoder {

	public OpsSatDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 9600, req.getBandwidth(), 0.175f * 3, 0.02f);
		SoftToHard bs = new SoftToHard(demodulator);
		return new OpsSat(bs);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return OpsSatBeacon.class;
	}
}
