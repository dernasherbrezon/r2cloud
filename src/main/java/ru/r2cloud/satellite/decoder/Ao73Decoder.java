package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.ao40.Ao40CorrelateAccessCodeTag;
import ru.r2cloud.jradio.ao73.Ao73;
import ru.r2cloud.jradio.blocks.ComplexToReal;
import ru.r2cloud.jradio.blocks.DelayOne;
import ru.r2cloud.jradio.blocks.FLLBandEdge;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.PolyphaseClockSyncComplex;
import ru.r2cloud.jradio.blocks.RmsAgc;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;

public class Ao73Decoder extends TelemetryDecoder {

	public Ao73Decoder(Configuration config, Predict predict) {
		super(config, predict);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int nfilts = 16;
		float[] taps = Firdes.lowPass(1.0, source.getContext().getSampleRate(), 1300, 500, Window.WIN_HAMMING, 6.76);
		FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(source, taps, 5, 1500);
		RmsAgc agc = new RmsAgc(xlating, 1e-2f, 0.5f);
		float samplesPerSymbol = agc.getContext().getSampleRate() / 1200.0f;
		FLLBandEdge fll = new FLLBandEdge(agc, samplesPerSymbol, 0.35f, 100, 0.01f);
		float[] rrcTaps = Firdes.rootRaisedCosine(nfilts, nfilts, 1.0f / samplesPerSymbol, 0.35f, (int) (11 * samplesPerSymbol * nfilts));
		PolyphaseClockSyncComplex clock = new PolyphaseClockSyncComplex(fll, samplesPerSymbol, 0.1f, rrcTaps, nfilts, nfilts / 2, 0.05f);
		DelayOne delay = new DelayOne(clock);
		ComplexToReal complexToReal = new ComplexToReal(delay);
		FloatToChar f2char = new FloatToChar(complexToReal, 127.0f);
		Ao40CorrelateAccessCodeTag tag = new Ao40CorrelateAccessCodeTag(f2char, 8);
		return new Ao73(tag);
	}

}
