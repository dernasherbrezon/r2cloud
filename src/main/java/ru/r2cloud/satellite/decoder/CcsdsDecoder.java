package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.ccsds.CcsdsBeaconSource;
import ru.r2cloud.jradio.ccsds.CcsdsFraming;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class CcsdsDecoder extends TelemetryDecoder {

	private final Class<? extends Beacon> beacon;
	private final CcsdsFraming framing;

	public CcsdsDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> beacon, CcsdsFraming framing) {
		super(predict, config);
		this.beacon = beacon;
		this.framing = framing;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		return new CcsdsBeaconSource<>(demodulator, framing, beacon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
