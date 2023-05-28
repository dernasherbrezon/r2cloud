package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.cc11xx.Cc11xxBeaconSource;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Cc11xxDecoder extends TelemetryDecoder {

	private final static int DEFAULT_BEACON_LENGTH_BYTES = 120;
	private final Class<? extends Beacon> clazz;
	private final String syncword;
	private final int beaconLength;

	public Cc11xxDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> clazz) {
		this(predict, config, clazz, DEFAULT_BEACON_LENGTH_BYTES);
	}

	public Cc11xxDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> clazz, int beaconLength) {
		// sync1 sync0: 0xd3 0x91 0xd3 0x91 according
		// https://www.usconverters.com/downloads/datasheets/cc1101.pdf
		this(predict, config, clazz, "11010011100100011101001110010001", beaconLength);
	}

	public Cc11xxDecoder(PredictOreKit predict, Configuration config, Class<? extends Beacon> clazz, String syncword, int beaconLength) {
		super(predict, config);
		this.clazz = clazz;
		this.syncword = syncword;
		if (beaconLength == 0) {
			// beacon length in bytes is not a required field in the transmitter definition
			this.beaconLength = DEFAULT_BEACON_LENGTH_BYTES;
		} else {
			this.beaconLength = beaconLength;
		}
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		return new Cc11xxBeaconSource<>(demodulator, clazz, syncword, beaconLength, true, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
