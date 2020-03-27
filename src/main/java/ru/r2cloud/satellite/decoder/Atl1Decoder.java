package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.atl1.Atl1;
import ru.r2cloud.jradio.atl1.Atl1Beacon;
import ru.r2cloud.jradio.atl1.Atl1RaCoded;
import ru.r2cloud.jradio.atl1.Atl1Short;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Atl1Decoder extends TelemetryDecoder {

	private static final int[] DOWNLINK_SPEEDS = new int[] { 1250, 2500, 5000, 12500 };

	public Atl1Decoder(Configuration config) {
		super(config);
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		for (int i = 0; i < DOWNLINK_SPEEDS.length; i++) {
			CorrelateAccessCodeTag correlateTag128 = new CorrelateAccessCodeTag(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 0, "0010110111010100", true);
			Atl1RaCoded raCoded128 = new Atl1RaCoded(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag128, 260 * 8)), 128);
			result.add(raCoded128);

			CorrelateAccessCodeTag correlateTag256 = new CorrelateAccessCodeTag(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 0, "0010110111010100", true);
			Atl1RaCoded raCoded256 = new Atl1RaCoded(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag256, 514 * 8)), 256);
			result.add(raCoded256);

			result.add(new Atl1Short(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));

			result.add(new Atl1(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));
		}
		return result;
	}

	private static GmskDemodulator createDemodulator(int downlinkSpeed, File rawIq, ObservationRequest req) throws IOException {
		DopplerCorrectedSource source = new DopplerCorrectedSource(rawIq, req);
		return new GmskDemodulator(source, downlinkSpeed, downlinkSpeed * 2.0f, 0.175f * 3, 0.03f);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Atl1Beacon.class;
	}

}
