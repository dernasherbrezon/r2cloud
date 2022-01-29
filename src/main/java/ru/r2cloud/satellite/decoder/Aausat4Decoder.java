package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.aausat4.Aausat4;
import ru.r2cloud.jradio.aausat4.Aausat4Beacon;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Aausat4Decoder extends TelemetryDecoder {

	public Aausat4Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		Integer baudRate = req.getBaudRates().get(0);
		GmskDemodulator demod = new GmskDemodulator(source, baudRate, req.getBandwidth(), 0.175f, 0.06f, Util.convertDecimation(baudRate), 2000);
		CorrelateSyncword correlate = new CorrelateSyncword(demod, 10, "010011110101101000110100010000110101010101000010", Aausat4.VITERBI_TAIL_SIZE + 8);
		return new Aausat4(correlate); // 8 for fsm
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Aausat4Beacon.class;
	}

}
