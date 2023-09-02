package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.aausat4.Aausat4;
import ru.r2cloud.jradio.aausat4.Aausat4Beacon;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Aausat4Decoder extends TelemetryDecoder {

	public Aausat4Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		CorrelateSyncword correlate = new CorrelateSyncword(demodulator, 10, "010011110101101000110100010000110101010101000010", Aausat4.VITERBI_TAIL_SIZE + 8);
		return new Aausat4(correlate); // 8 for fsm
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Aausat4Beacon.class;
	}

}
