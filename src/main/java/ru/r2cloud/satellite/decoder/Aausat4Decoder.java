package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.aausat4.AAUSAT4;
import ru.r2cloud.jradio.aausat4.AAUSAT4Beacon;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.LowPassFilter;
import ru.r2cloud.jradio.blocks.MultiplyConst;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Aausat4Decoder extends TelemetryDecoder {

	public Aausat4Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		QuadratureDemodulation qd = new QuadratureDemodulation(source, 0.4f);
		LowPassFilter lpf = new LowPassFilter(qd, 1.0, 1500.0f, 100, Window.WIN_HAMMING, 6.76);
		MultiplyConst mc = new MultiplyConst(lpf, 1.0f);
		ClockRecoveryMM clockRecovery = new ClockRecoveryMM(mc, mc.getContext().getSampleRate() / 2400, (float) (0.25 * 0.175 * 0.175), 0.005f, 0.175f, 0.005f);
		Rail rail = new Rail(clockRecovery, -1.0f, 1.0f);
		FloatToChar f2char = new FloatToChar(rail, 127.0f);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(f2char, 10, "010011110101101000110100010000110101010101000010", true);
		return new AAUSAT4(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, AAUSAT4.VITERBI_TAIL_SIZE + 8))); // 8 for fsm
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return AAUSAT4Beacon.class;
	}

}
