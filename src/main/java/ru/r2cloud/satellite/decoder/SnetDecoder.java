package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.snet.Snet;
import ru.r2cloud.jradio.snet.SnetBeacon;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class SnetDecoder extends TelemetryDecoder {

	public SnetDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		CorrelateSyncword correlateTag = new CorrelateSyncword(new SoftToHard(demodulator), 4, "00000100110011110101111111001000", 512 * 8);
		return new Snet(correlateTag);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return SnetBeacon.class;
	}
}
