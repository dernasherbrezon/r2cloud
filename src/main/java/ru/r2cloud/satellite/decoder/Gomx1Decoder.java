package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax100BeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.gomx1.Gomx1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Gomx1Decoder extends TelemetryDecoder {

	private final Class<? extends Beacon> clazz;
	private final boolean forceViterbi;
	private final boolean forceScrambler;
	private final boolean forceReedSolomon;

	public Gomx1Decoder(PredictOreKit predict, Configuration config) {
		this(predict, config, Gomx1Beacon.class, false, false, false);
	}

	public Gomx1Decoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> clazz, boolean forceViterbi, boolean forceScrambler, boolean forceReedSolomon) {
		super(predict, config);
		this.clazz = clazz;
		this.forceViterbi = forceViterbi;
		this.forceScrambler = forceScrambler;
		this.forceReedSolomon = forceReedSolomon;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		AfskDemodulator demod = new AfskDemodulator(source, baudRate, -1200, 3600, Util.convertDecimation(baudRate));
		return new Ax100BeaconSource<>(demod, 255, "11000011101010100110011001010101", clazz, forceViterbi, forceScrambler, forceReedSolomon);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
