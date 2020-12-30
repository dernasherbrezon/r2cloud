package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.cc11xx.Cc11xxReceiver;
import ru.r2cloud.jradio.rhw.ReaktorHelloWorld;
import ru.r2cloud.jradio.rhw.ReaktorHelloWorldBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class ReaktorHelloWorldDecoder extends TelemetryDecoder {

	public ReaktorHelloWorldDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		GmskDemodulator gmsk = new GmskDemodulator(source, 9600, req.getBandwidth(), gainMu);
		SoftToHard s2h = new SoftToHard(gmsk);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 8, "00110101001011100011010100101110", 120 * 8);
		Cc11xxReceiver cc11 = new Cc11xxReceiver(correlate, true, true);
		return new ReaktorHelloWorld(cc11);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return ReaktorHelloWorldBeacon.class;
	}
}
