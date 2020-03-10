package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.jradio.lume1.Lume1;
import ru.r2cloud.jradio.lume1.Lume1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Lume1Decoder extends TelemetryDecoder {

	public Lume1Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 4800, req.getBandwidth(), gainMu, 0.01f);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(gmsk, 6, "10010011000010110101000111011110", true);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, 255 * 8));
		AX100Decoder ax100 = new AX100Decoder(pdu, false, true, true);
		return new Lume1(ax100);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Lume1Beacon.class;
	}

}
