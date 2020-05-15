package ru.r2cloud.satellite.decoder;

import java.util.HashSet;
import java.util.Set;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.fox.Fox;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class FoxSlowDecoder<T extends Beacon> extends TelemetryDecoder {

	private final Class<T> clazz;

	public FoxSlowDecoder(PredictOreKit predict, Configuration config, Class<T> clazz) {
		super(predict, config);
		this.clazz = clazz;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 200, req.getBandwidth(), gainMu, null, 120, 200.0f);
		SoftToHard s2h = new SoftToHard(gmsk);
		Set<String> codes = new HashSet<>();
		codes.add("0011111010");
		codes.add("1100000101");
		CorrelateAccessCodeTag correlate = new CorrelateAccessCodeTag(s2h, 0, codes, false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlate, Fox.SLOW_FRAME_SIZE * 10));
		return new Fox<T>(pdu, clazz);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
