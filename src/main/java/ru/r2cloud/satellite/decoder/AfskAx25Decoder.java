package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class AfskAx25Decoder extends TelemetryDecoder {

	private final int baudRate;
	private final Class<? extends Beacon> beacon;

	public AfskAx25Decoder(PredictOreKit predict, Configuration config, int baudRate, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.baudRate = baudRate;
		this.beacon = beacon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		AfskDemodulator demodulator = new AfskDemodulator(source, baudRate, 500, 1700, 5);
		return new Ax25BeaconSource<>(demodulator, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
