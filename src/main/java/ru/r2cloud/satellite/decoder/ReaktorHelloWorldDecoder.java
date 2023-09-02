package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.cc11xx.Cc11xxBeaconSource;
import ru.r2cloud.jradio.rhw.ReaktorHelloWorldBeacon;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class ReaktorHelloWorldDecoder extends TelemetryDecoder {

	public ReaktorHelloWorldDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Cc11xxBeaconSource<>(demodulator, ReaktorHelloWorldBeacon.class, "00110101001011100011010100101110", 512, true, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return ReaktorHelloWorldBeacon.class;
	}
}
