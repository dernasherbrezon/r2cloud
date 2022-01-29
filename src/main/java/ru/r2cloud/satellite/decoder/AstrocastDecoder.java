package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.astrocast.Astrocast9k6;
import ru.r2cloud.jradio.astrocast.Astrocast9k6Beacon;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class AstrocastDecoder extends TelemetryDecoder {

	public AstrocastDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator gmsk = new GmskDemodulator(source, req.getBaudRates().get(0), req.getBandwidth(), 0.175f * 5);
		SoftToHard s2h = new SoftToHard(gmsk);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 4, "00011010110011111111110000011101", 255 * 8 * 5);
		return new Astrocast9k6(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Astrocast9k6Beacon.class;
	}

}
