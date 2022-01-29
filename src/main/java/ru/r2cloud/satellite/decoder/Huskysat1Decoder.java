package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.jradio.huskysat1.Huskysat1;
import ru.r2cloud.jradio.huskysat1.Huskysat1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Huskysat1Decoder extends TelemetryDecoder {

	public Huskysat1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		BpskDemodulator bpsk = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), 1200, true);
		SoftToHard s2h = new SoftToHard(bpsk);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 4, "1000111110011010010000101011101", Huskysat1.FRAME_SIZE * 10);
		return new Huskysat1(correlate);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Huskysat1Beacon.class;
	}

}
