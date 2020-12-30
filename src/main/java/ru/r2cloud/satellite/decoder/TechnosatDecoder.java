package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.technosat.Technosat;
import ru.r2cloud.jradio.technosat.TechnosatBeacon;
import ru.r2cloud.jradio.tubix20.CMX909bBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class TechnosatDecoder extends TelemetryDecoder {

	public TechnosatDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f;
		GmskDemodulator gmsk = new GmskDemodulator(source, 4800, req.getBandwidth(), gainMu);
		CorrelateSyncword correlateTag = new CorrelateSyncword(gmsk, 4, "111011110000111011110000", CMX909bBeacon.MAX_SIZE * 8);
		return new Technosat(correlateTag);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return TechnosatBeacon.class;
	}

}
