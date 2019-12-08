package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.ComplexConjugate;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.LowPassFilterComplex;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.jradio.gomx1.Gomx1;
import ru.r2cloud.jradio.gomx1.Gomx1Beacon;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class Gomx1Decoder extends TelemetryDecoder {

	public Gomx1Decoder(Configuration config) {
		super(config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		float gainMu = 0.175f;
		SigSource sigSource = new SigSource(Waveform.COMPLEX, (long) source.getContext().getSampleRate(), -3600.0f, 1.0);
		Multiply multiply = new Multiply(source, sigSource);
		ComplexConjugate cc = new ComplexConjugate(multiply);
		LowPassFilterComplex lpf = new LowPassFilterComplex(cc, 1.0, 2600, 1000, Window.WIN_HAMMING, 6.76);

		float samplesPerSymbol = lpf.getContext().getSampleRate() / 4800;
		float sensitivity = (float) ((Math.PI / 2) / samplesPerSymbol);
		float demodGain = (float) (1.0 / sensitivity);
		QuadratureDemodulation qd = new QuadratureDemodulation(lpf, demodGain);
		ClockRecoveryMM clockRecovery = new ClockRecoveryMM(qd, samplesPerSymbol, (float) (0.25 * gainMu * gainMu), 0.5f, gainMu, 0.005f);
		Rail rail = new Rail(clockRecovery, -1.0f, 1.0f);
		FloatToChar f2char = new FloatToChar(rail, 127.0f);
		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(f2char, 4, "11000011101010100110011001010101", true);
		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, (255 + 3) * 8));
		AX100Decoder ax100 = new AX100Decoder(pdu, false, false, false);
		return new Gomx1(ax100);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Gomx1Beacon.class;
	}
}
