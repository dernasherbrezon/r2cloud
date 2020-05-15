package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.fox.HighSpeedFox;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class FoxDecoder<T extends Beacon> extends FoxSlowDecoder<T> {

	private final Class<T> clazz;

	public FoxDecoder(PredictOreKit predict, Configuration config, Class<T> clazz) {
		super(predict, config, clazz);
		this.clazz = clazz;
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		// slow fox
		DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req);
		result.add(createBeaconSource(source, req));

		DopplerCorrectedSource source2 = new DopplerCorrectedSource(predict, rawIq, req);
		GmskDemodulator gmsk = new GmskDemodulator(source2, 9600, req.getBandwidth(), 0.175f * 3);
		SoftToHard s2h = new SoftToHard(gmsk);
		Set<String> codes = new HashSet<>();
		codes.add("0011111010");
		codes.add("1100000101");
		CorrelateAccessCodeTag correlate = new CorrelateAccessCodeTag(s2h, 0, codes, false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlate, HighSpeedFox.HIGH_SPEED_FRAME_SIZE * 10));
		result.add(new HighSpeedFox(pdu));

		return result;
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}

}
