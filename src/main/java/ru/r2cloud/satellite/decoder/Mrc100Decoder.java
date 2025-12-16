package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.mrc100.Mrc100;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Mrc100Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;

	public Mrc100Decoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.beacon = beacon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Mrc100<>(demodulator, 126, 260, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
