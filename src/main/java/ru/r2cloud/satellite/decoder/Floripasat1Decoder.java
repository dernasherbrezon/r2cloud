package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.jradio.florsat.Floripasat1;
import ru.r2cloud.jradio.florsat.Floripasat1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Floripasat1Decoder extends TelemetryDecoder {

	public Floripasat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 1200, req.getBandwidth(), 0.175f * 3);
		SoftToHard bs = new SoftToHard(demodulator);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(bs, 5, "01011101111001100010101001111110", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, (255 + 3) * 8), 1, Endianness.GR_MSB_FIRST));
		return new Floripasat1(pdu);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Floripasat1Beacon.class;
	}

}
