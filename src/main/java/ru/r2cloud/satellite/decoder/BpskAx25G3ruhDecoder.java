package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class BpskAx25G3ruhDecoder extends TelemetryDecoder {

	private final int baudRate;
	private final Class<? extends Beacon> beacon;

	public BpskAx25G3ruhDecoder(PredictOreKit predict, Configuration config, int baudRate, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.baudRate = baudRate;
		this.beacon = beacon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		BpskDemodulator demodulator = new BpskDemodulator(source, baudRate, 1, 0, false);
		return new Ax25G3ruhBeaconSource<>(demodulator, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
