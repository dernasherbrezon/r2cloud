package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.aausat4.Aausat4;
import ru.r2cloud.jradio.aausat4.Aausat4Beacon;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Aausat4Decoder extends TelemetryDecoder {

	public Aausat4Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f;
		GmskDemodulator gmsk = new GmskDemodulator(source, 2400, req.getBandwidth(), gainMu, 0.06f, 4, 2000);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(gmsk, 10, "010011110101101000110100010000110101010101000010", true);
		return new Aausat4(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, Aausat4.VITERBI_TAIL_SIZE + 8))); // 8 for fsm
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Aausat4Beacon.class;
	}

}
