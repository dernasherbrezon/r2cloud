package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.technosat.Technosat;
import ru.r2cloud.jradio.technosat.TechnosatBeacon;
import ru.r2cloud.jradio.tubix20.CMX909bBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class TechnosatDecoder extends TelemetryDecoder {

	public TechnosatDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		FskDemodulator demod = new FskDemodulator(source, 4800, 5000.0f, Util.convertDecimation(4800), 2000, true);
		CorrelateSyncword correlateTag = new CorrelateSyncword(demod, 4, "111011110000111011110000", CMX909bBeacon.MAX_SIZE * 8);
		return new Technosat(correlateTag);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return TechnosatBeacon.class;
	}

}
