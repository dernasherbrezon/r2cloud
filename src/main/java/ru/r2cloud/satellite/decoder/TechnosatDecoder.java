package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.InvertBits;
import ru.r2cloud.jradio.blocks.SoftToHard;
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
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		SoftToHard s2h = new SoftToHard(demodulator);
		InvertBits invert = new InvertBits(s2h);		
		CorrelateSyncword correlateTag = new CorrelateSyncword(invert, 4, "111011110000111011110000", CMX909bBeacon.MAX_SIZE * 8);
		return new Technosat(correlateTag);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return TechnosatBeacon.class;
	}

}
