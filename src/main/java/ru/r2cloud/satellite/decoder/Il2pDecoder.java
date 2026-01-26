package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.Il2pBeaconSource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Il2pDecoder extends TelemetryDecoder {

	private final int beaconSize;
	private final Class<? extends Beacon> beacon;

	public Il2pDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, int beaconSize) {
		super(predict, config);
		this.beacon = beacon;
		this.beaconSize = beaconSize == 0 ? 512 : beaconSize;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new Il2pBeaconSource<>(demodulator, beaconSize, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
