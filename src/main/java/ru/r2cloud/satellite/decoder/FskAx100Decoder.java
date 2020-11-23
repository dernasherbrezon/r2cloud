package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class FskAx100Decoder extends TelemetryDecoder {

	private final int baudRate;
	private final Class<? extends Beacon> beacon;
	private final int beaconSizeBytes;

	public FskAx100Decoder(PredictOreKit predict, Configuration config, int baudRate, int beaconSizeBytes, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.baudRate = baudRate;
		this.beacon = beacon;
		this.beaconSizeBytes = beaconSizeBytes;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator demodulator = new GmskDemodulator(source, baudRate, req.getBandwidth(), gainMu, 0.02f, Util.convertDecimation(baudRate), 2000);
		return new Ax100BeaconSource<>(demodulator, beaconSizeBytes, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}
}
