package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.InvertBits;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.dstar1.Dstar1;
import ru.r2cloud.jradio.dstar1.Dstar1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Dstar1Decoder extends TelemetryDecoder {

	private static final int MAX_MESSAGE_SIZE_BYTES = 190;

	public Dstar1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		SoftToHard s2h = new SoftToHard(demodulator);
		InvertBits invert = new InvertBits(s2h);
		CorrelateSyncword correlate = new CorrelateSyncword(invert, 6, "1100110011000101011101100101", MAX_MESSAGE_SIZE_BYTES * 8);
		return new Dstar1(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Dstar1Beacon.class;
	}
}
