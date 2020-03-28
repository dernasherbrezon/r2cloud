package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.InvertBits;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
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
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f;
		GmskDemodulator gmsk = new GmskDemodulator(source, 4800, req.getBandwidth(), gainMu);
		SoftToHard s2h = new SoftToHard(gmsk);
		InvertBits invert = new InvertBits(s2h);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(invert, 6, "1100110011000101011101100101", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, MAX_MESSAGE_SIZE_BYTES * 8), 1, Endianness.GR_MSB_FIRST));
		return new Dstar1(pdu);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Dstar1Beacon.class;
	}
}
