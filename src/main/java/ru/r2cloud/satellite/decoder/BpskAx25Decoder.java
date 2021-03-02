package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class BpskAx25Decoder extends TelemetryDecoder {

	private final int baudRate;
	private final double centerFrequency;
	private final Class<? extends Beacon> beacon;

	public BpskAx25Decoder(PredictOreKit predict, Configuration config, int baudRate, Class<? extends Beacon> beacon) {
		this(predict, config, baudRate, 0.0, beacon);
	}

	public BpskAx25Decoder(PredictOreKit predict, Configuration config, int baudRate, double centerFrequency, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.baudRate = baudRate;
		this.centerFrequency = centerFrequency;
		this.beacon = beacon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator demodulator = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), centerFrequency, false);
		return new Ax25BeaconSource<>(demodulator, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
