package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.astrocast.Astrocast;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;

public class AstrocastDecoder extends TelemetryDecoder {

	public AstrocastDecoder(Configuration config, Predict predict) {
		super(config, predict);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 5;
		int baud = 1200;
		GmskDemodulator gmsk = new GmskDemodulator(source, baud, gainMu);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(gmsk, 8, "0111010111111010110000011010001101011000110100000110010001110110", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 255 * 8), 1, Endianness.GR_MSB_FIRST));
		return new Astrocast(pdu);
	}

}
