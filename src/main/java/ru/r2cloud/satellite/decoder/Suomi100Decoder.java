package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.jradio.suomi100.Suomi100;
import ru.r2cloud.jradio.suomi100.Suomi100Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Suomi100Decoder extends TelemetryDecoder {

	public Suomi100Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 9600, req.getBandwidth(), gainMu);
		CorrelateSyncword correlateTag = new CorrelateSyncword(gmsk, 4, "10010011000010110101000111011110", 255 * 8);
		AX100Decoder ax100 = new AX100Decoder(correlateTag, false, true, true);
		return new Suomi100(ax100);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Suomi100Beacon.class;
	}
}
