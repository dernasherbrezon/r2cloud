package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class AfskAx25Decoder extends TelemetryDecoder {

	private final float deviation;
	private final Class<? extends Beacon> beacon;
	private final byte[] assistedHeader;

	public AfskAx25Decoder(PredictOreKit predict, Configuration config, float deviation, Class<? extends Beacon> beacon, byte[] assistedHeader) {
		super(predict, config);
		this.beacon = beacon;
		this.deviation = deviation;
		this.assistedHeader = assistedHeader;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		AfskDemodulator demodulator = new AfskDemodulator(source, baudRate, deviation, 1700, Util.convertDecimation(baudRate));
		return new Ax25BeaconSource<>(demodulator, beacon, true, assistedHeader);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
