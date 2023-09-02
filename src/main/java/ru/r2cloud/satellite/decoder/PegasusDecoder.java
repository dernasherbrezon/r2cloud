package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.at03.At03;
import ru.r2cloud.jradio.at03.At03Beacon;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class PegasusDecoder extends TelemetryDecoder {

	public PegasusDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		SoftToHard s2h = new SoftToHard(demodulator);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 2, "0010110111010100", 64 * 8);
		return new At03(correlate);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return At03Beacon.class;
	}
}
