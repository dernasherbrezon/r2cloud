package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.is7.InspireSat7Beacon;
import ru.r2cloud.jradio.is7.InspireSat7Spino;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class InspireSat7SpinoDecoder extends TelemetryDecoder {

	public InspireSat7SpinoDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new InspireSat7Spino(demodulator);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return InspireSat7Beacon.class;
	}
}
