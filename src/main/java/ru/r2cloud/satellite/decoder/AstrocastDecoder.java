package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.astrocast.Astrocast9k6;
import ru.r2cloud.jradio.astrocast.Astrocast9k6Beacon;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class AstrocastDecoder extends TelemetryDecoder {

	public AstrocastDecoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 5;
		int baud = 9600;
		GmskDemodulator gmsk = new GmskDemodulator(source, baud, req.getBandwidth(), gainMu);
		SoftToHard s2h = new SoftToHard(gmsk);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(s2h, 4, "00011010110011111111110000011101", false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 255 * 8 * 5), 1, Endianness.GR_MSB_FIRST));
		return new Astrocast9k6(pdu);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Astrocast9k6Beacon.class;
	}

}
