package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class FskAx100Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;
	private final int beaconSizeBytes;

	public FskAx100Decoder(PredictOreKit predict, Configuration config, int beaconSizeBytes, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.beacon = beacon;
		this.beaconSizeBytes = beaconSizeBytes;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, ObservationRequest req) {
		return new Ax100BeaconSource<>(source, beaconSizeBytes, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}
}
