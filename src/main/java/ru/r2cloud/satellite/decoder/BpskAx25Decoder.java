package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.BpskDemodulator;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class BpskAx25Decoder extends TelemetryDecoder {

	private final double centerFrequency;
	private final Class<? extends Beacon> beacon;
	private final byte[] assistedHeader;

	public BpskAx25Decoder(PredictOreKit predict, Configuration config, double centerFrequency, Class<? extends Beacon> beacon, byte[] assistedHeader) {
		super(predict, config);
		this.centerFrequency = centerFrequency;
		this.beacon = beacon;
		this.assistedHeader = assistedHeader;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		BpskDemodulator demodulator = new BpskDemodulator(source, baudRate, Util.convertDecimation(baudRate), centerFrequency, false);
		return new Ax25BeaconSource<>(demodulator, beacon, true, assistedHeader);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return beacon;
	}

}
