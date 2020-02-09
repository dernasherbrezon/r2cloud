package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.aistechsat3.Aistechsat3;
import ru.r2cloud.jradio.aistechsat3.Aistechsat3Beacon;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Aistechsat3Decoder extends TelemetryDecoder {

	public Aistechsat3Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 9600, req.getBandwidth(), 0.175f * 3);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(demodulator, 4, "10010011000010110101000111011110", true);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, (255 + 3) * 8));
		AX100Decoder ax100 = new AX100Decoder(pdu, false, true, true);
		return new Aistechsat3(ax100);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Aistechsat3Beacon.class;
	}

}
