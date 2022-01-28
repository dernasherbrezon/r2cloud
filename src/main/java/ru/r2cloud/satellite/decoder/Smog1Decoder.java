package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.smog1.Smog1;
import ru.r2cloud.jradio.smog1.Smog1Beacon;
import ru.r2cloud.jradio.smog1.Smog1RaCoded;
import ru.r2cloud.jradio.smog1.Smog1Short;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Smog1Decoder extends TelemetryDecoder {

	private static final int[] DOWNLINK_SPEEDS = new int[] { 1250, 2500, 5000, 12500 };

	public Smog1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		for (int i = 0; i < DOWNLINK_SPEEDS.length; i++) {
			result.add(new Smog1RaCoded(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 128, 260));
			result.add(new Smog1RaCoded(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req), 256, 514));
			result.add(new Smog1Short(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));
			result.add(new Smog1(createDemodulator(DOWNLINK_SPEEDS[i], rawIq, req)));
		}
		return result;
	}

	private ByteInput createDemodulator(int downlinkSpeed, File rawIq, ObservationRequest req) throws IOException {
		DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req);
		return new FskDemodulator(source, downlinkSpeed, 5000.0f, Util.convertDecimation(downlinkSpeed), 2000, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Smog1Beacon.class;
	}

}
