package ru.r2cloud.satellite.decoder;

import java.util.HashSet;
import java.util.Set;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.fox.Fox;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class FoxSlowDecoder<T extends Beacon> extends TelemetryDecoder {

	private final Class<T> clazz;

	public FoxSlowDecoder(PredictOreKit predict, Configuration config, Class<T> clazz) {
		super(predict, config);
		this.clazz = clazz;
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, ObservationRequest req) {
		SoftToHard s2h = new SoftToHard(demodulator);
		Set<String> codes = new HashSet<>();
		codes.add("0011111010");
		codes.add("1100000101");
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 2, codes, Fox.SLOW_FRAME_SIZE * 10);
		return new Fox<>(correlate, clazz);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
