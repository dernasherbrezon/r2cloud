package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class FskAx100Decoder extends TelemetryDecoder {

	private final int[] baudRate;
	private final Class<? extends Beacon> beacon;
	private final int beaconSizeBytes;

	public FskAx100Decoder(PredictOreKit predict, Configuration config, int beaconSizeBytes, Class<? extends Beacon> beacon, int... baudRate) {
		super(predict, config);
		this.baudRate = baudRate;
		this.beacon = beacon;
		this.beaconSizeBytes = beaconSizeBytes;
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		for (int i = 0; i < baudRate.length; i++) {
			DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req);
			FskDemodulator demodulator = new FskDemodulator(source, baudRate[i], 5000.0f, Util.convertDecimation(baudRate[i]), 2000);
			result.add(new Ax100BeaconSource<>(demodulator, beaconSizeBytes, beacon));
		}
		return result;
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}
}
