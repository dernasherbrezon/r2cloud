package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.BinarySlicer;
import ru.r2cloud.jradio.blocks.ComplexToReal;
import ru.r2cloud.jradio.blocks.CostasLoop;
import ru.r2cloud.jradio.blocks.Descrambler;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.HdlcReceiver;
import ru.r2cloud.jradio.blocks.NrziDecode;
import ru.r2cloud.jradio.blocks.PolyphaseClockSyncComplex;
import ru.r2cloud.jradio.blocks.RmsAgc;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.pwsat2.PwSat2;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;

public class PwSat2Decoder extends TelemetryDecoder {

	public PwSat2Decoder(Configuration config, Predict predict) {
		super(config, predict);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int nfilts = 16;
		float[] taps = Firdes.lowPass(1.0, source.getContext().getSampleRate(), 1300.0f, 500, Window.WIN_HAMMING, 6.76);
		float samplesPerSymbol = source.getContext().getSampleRate() / 5 / 1200.0f;
		float[] rrcTaps = Firdes.rootRaisedCosine(nfilts, nfilts, 1.0f / samplesPerSymbol, 0.35f, (int) (11 * samplesPerSymbol * nfilts));
		FrequencyXlatingFIRFilter freq = new FrequencyXlatingFIRFilter(source, taps, 5, 1500);
		RmsAgc agc = new RmsAgc(freq, 1e-2f, 0.5f);
		PolyphaseClockSyncComplex clockSync = new PolyphaseClockSyncComplex(agc, samplesPerSymbol, 0.05f, rrcTaps, nfilts, nfilts / 2, 0.05f);
		CostasLoop costas = new CostasLoop(clockSync, 0.5f, 2, false);
		ComplexToReal c2r = new ComplexToReal(costas);
		BinarySlicer bs = new BinarySlicer(c2r);
		NrziDecode nrzi = new NrziDecode(bs);
		Descrambler descrambler = new Descrambler(nrzi, 0x21, 0, 16);
		HdlcReceiver hdlc = new HdlcReceiver(descrambler, 10000);
		return new PwSat2(hdlc);
	}
}
