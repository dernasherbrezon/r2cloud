package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.lucky7.Lucky7;
import ru.r2cloud.jradio.lucky7.Lucky7Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Lucky7Decoder extends TelemetryDecoder {

	public Lucky7Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 4800, req.getBandwidth(), 0.175f, null);
		SoftToHard bs = new SoftToHard(demodulator);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(bs, 3, "0010110111010100", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, 37 * 8));
		return new Lucky7(pdu);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Lucky7Beacon.class;
	}
}