package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Ax100Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;
	private final String syncword;

	public Ax100Decoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, String syncword) {
		super(predict, config);
		this.beacon = beacon;
		this.syncword = syncword;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput source, Observation req) {
		return new Ax100BeaconSource<>(source, 512, syncword, beacon, false, true, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}
}
