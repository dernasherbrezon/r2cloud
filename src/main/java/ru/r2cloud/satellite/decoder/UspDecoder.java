package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.usp.UspBeaconSource;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class UspDecoder extends TelemetryDecoder {

	private final int baudRate;
	private final Class<? extends Beacon> beacon;

	public UspDecoder(PredictOreKit predict, Configuration config, int baudRate, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.baudRate = baudRate;
		this.beacon = beacon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		FskDemodulator demodulator = new FskDemodulator(source, baudRate, 5000.0f, Util.convertDecimation(baudRate), 1000);
		return new UspBeaconSource<>(demodulator, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
