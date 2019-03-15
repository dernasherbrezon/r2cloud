package ru.r2cloud.satellite.decoder;

import java.io.IOException;

import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.Window;

public class GmskDemodulator implements ByteInput {

	private final ByteInput source;

	public GmskDemodulator(FloatInput source, double centerFreq, int baudRate, double cutoffFreq, float gainMu) {
		float[] taps = Firdes.lowPass(1.0, source.getContext().getSampleRate(), cutoffFreq, 600, Window.WIN_HAMMING, 6.76);
		FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(source, taps, 2, centerFreq);

		float samplesPerSymbol = xlating.getContext().getSampleRate() / baudRate;
		float sensitivity = (float) ((Math.PI / 2) / samplesPerSymbol);

		QuadratureDemodulation qd = new QuadratureDemodulation(xlating, 1 / sensitivity);
		ClockRecoveryMM clockRecovery = new ClockRecoveryMM(qd, samplesPerSymbol, (float) (0.25 * gainMu * gainMu), 0.5f, gainMu, 0.005f);
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
