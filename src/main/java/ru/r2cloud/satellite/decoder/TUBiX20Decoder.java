package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.tubix20.TUBiX20BeaconSource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class TUBiX20Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;

	public TUBiX20Decoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon) {
		super(predict, config);
		this.beacon = beacon;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new TUBiX20BeaconSource(demodulator, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
