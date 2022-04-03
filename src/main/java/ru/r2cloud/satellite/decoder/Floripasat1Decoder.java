package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.florsat.Floripasat1;
import ru.r2cloud.jradio.florsat.Floripasat1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Floripasat1Decoder extends TelemetryDecoder {

	public Floripasat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		SoftToHard bs = new SoftToHard(demodulator);
		CorrelateSyncword correlate = new CorrelateSyncword(bs, 5, "01011101111001100010101001111110", (255 + 3) * 8);
		return new Floripasat1(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Floripasat1Beacon.class;
	}

}
