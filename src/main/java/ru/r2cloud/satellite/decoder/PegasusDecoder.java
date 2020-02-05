package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.at03.At03;
import ru.r2cloud.jradio.at03.At03Beacon;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class PegasusDecoder extends TelemetryDecoder {

	public PegasusDecoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 9600, req.getBandwidth(), gainMu);
		SoftToHard s2h = new SoftToHard(gmsk);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(s2h, 2, "0010110111010100", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 64 * 8), 1, Endianness.GR_MSB_FIRST));
		return new At03(pdu);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return At03Beacon.class;
	}
}
