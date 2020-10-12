package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.smogp.SmogP;
import ru.r2cloud.jradio.smogp.SmogPBeacon;
import ru.r2cloud.jradio.smogp.SmogPRaCoded;
import ru.r2cloud.jradio.smogp.SmogPShort;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class SmogPDecoder extends TelemetryDecoder {

	private static final int[] DOWNLINK_SPEEDS = new int[] { 1250, 2500, 5000, 12500 };

	public SmogPDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		for (int i = 0; i < DOWNLINK_SPEEDS.length; i++) {
			CorrelateAccessCodeTag correlateTag128 = new CorrelateAccessCodeTag(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 0, "0010110111010100", true);
			SmogPRaCoded raCoded128 = new SmogPRaCoded(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag128, 260 * 8)), 128);
			result.add(raCoded128);

			CorrelateAccessCodeTag correlateTag256 = new CorrelateAccessCodeTag(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 0, "0010110111010100", true);
			SmogPRaCoded raCoded256 = new SmogPRaCoded(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag256, 514 * 8)), 256);
			result.add(raCoded256);

			result.add(new SmogPShort(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));

			result.add(new SmogP(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));
		}
		return result;
	}

	private GmskDemodulator createDemodulator(int downlinkSpeed, File rawIq, ObservationRequest req) throws IOException {
		DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req);
		return new GmskDemodulator(source, downlinkSpeed, downlinkSpeed * 2.0f, 0.175f * 3, 0.045f, 1, 2000);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return SmogPBeacon.class;
	}

}
