package ru.r2cloud.satellite.decoder;

import java.util.HashSet;
import java.util.Set;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.DcBlocker;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.LowPassFilter;
import ru.r2cloud.jradio.blocks.LowPassFilterComplex;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
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
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f * 3;
		int baudRate = 200;
		long bandwidth = req.getBandwidth();
		int decimation = 120;
		float transitionWidth = 200.0f;
		FloatInput next = new LowPassFilterComplex(source, 1.0, bandwidth / 2, 600, Window.WIN_HAMMING, 6.76);
		next = new QuadratureDemodulation(next, 1.0f);
		next = new LowPassFilter(next, decimation, 1.0, (double) baudRate / 2, transitionWidth, Window.WIN_HAMMING, 6.76);
		next = new DcBlocker(next, (int) (Math.ceil(next.getContext().getSampleRate() / baudRate * 32)), true);		
		next = new ClockRecoveryMM(next, next.getContext().getSampleRate() / baudRate, (float) (0.25 * gainMu * gainMu), 0.5f, gainMu, 0.005f);
		next = new Rail(next, -1.0f, 1.0f);
		ByteInput byteInput = new FloatToChar(next, 127.0f);
		SoftToHard s2h = new SoftToHard(byteInput);
		Set<String> codes = new HashSet<>();
		codes.add("0011111010");
		codes.add("1100000101");
		CorrelateAccessCodeTag correlate = new CorrelateAccessCodeTag(s2h, 0, codes, false);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlate, Fox.SLOW_FRAME_SIZE * 10));
		return new Fox<T>(pdu, clazz);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}
}
