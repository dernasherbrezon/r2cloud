package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.Endianness;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.UnpackedToPacked;
import ru.r2cloud.jradio.eseo.Eseo;
import ru.r2cloud.jradio.eseo.EseoBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class EseoDecoder extends TelemetryDecoder {

	public EseoDecoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 4800, gainMu);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(gmsk, 1, EseoBeacon.FLAG, false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 257 * 8), 1, Endianness.GR_MSB_FIRST));
		return new Eseo(pdu);
	}

}
