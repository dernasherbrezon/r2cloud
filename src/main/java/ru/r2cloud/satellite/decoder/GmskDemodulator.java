package ru.r2cloud.satellite.decoder;

import java.io.IOException;

import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.FLLBandEdge;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.LowPassFilter;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.RmsAgc;
import ru.r2cloud.jradio.blocks.Window;

public class GmskDemodulator implements ByteInput {

	private final ByteInput source;

	public GmskDemodulator(FloatInput source, int baudRate, float bandwidth, float gainMu) {
		float samplesPerSymbol = source.getContext().getSampleRate() / baudRate;
		RmsAgc agc = new RmsAgc(source, 1e-2f, 0.5f);
		FLLBandEdge fll = new FLLBandEdge(agc, samplesPerSymbol, 0.35f, 100, 0.05f);
		QuadratureDemodulation qd = new QuadratureDemodulation(fll, 1.0f);
		LowPassFilter lpf = new LowPassFilter(qd, 1.0, bandwidth / 2, 600, Window.WIN_HAMMING, 6.76);
		ClockRecoveryMM clockRecovery = new ClockRecoveryMM(lpf, samplesPerSymbol, (float) (0.25 * gainMu * gainMu), 0.5f, gainMu, 0.005f);
		Rail rail = new Rail(clockRecovery, -1.0f, 1.0f);
		this.source = new FloatToChar(rail, 127.0f);
	}

	@Override
	public byte readByte() throws IOException {
		return source.readByte();
	}

	@Override
	public Context getContext() {
		return source.getContext();
	}

	@Override
	public void close() throws IOException {
		source.close();
	}

}
