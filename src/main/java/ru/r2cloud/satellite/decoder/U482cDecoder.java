package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class U482cDecoder extends TelemetryDecoder {

	private final Class<? extends Beacon> clazz;
	private final String syncword;

	public U482cDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> clazz, String syncword) {
		super(predict, config);
		this.clazz = clazz;
		this.syncword = syncword;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Ax100BeaconSource<>(demodulator, 512, syncword, clazz, false, true, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
