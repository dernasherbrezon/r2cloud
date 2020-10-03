package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.aistechsat2.Aistechsat2;
import ru.r2cloud.jradio.aistechsat2.Aistechsat2Beacon;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Aistechsat2Decoder extends TelemetryDecoder {

	public Aistechsat2Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 9600, req.getBandwidth(), 0.175f * 3);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(demodulator, 4, "10010011000010110101000111011110", true);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, (255 + 3) * 8));
		AX100Decoder ax100 = new AX100Decoder(pdu, false, true, true);
		return new Aistechsat2(ax100);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Aistechsat2Beacon.class;
	}

}
