package ru.r2cloud.satellite.decoder;

import java.io.IOException;

import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;

public class GmskDemodulator implements ByteInput {

	private final ByteInput source;

	public GmskDemodulator(FloatInput source, int baudRate, float gainMu) {
		float samplesPerSymbol = source.getContext().getSampleRate() / baudRate;
		float sensitivity = (float) ((Math.PI / 2) / samplesPerSymbol);

		QuadratureDemodulation qd = new QuadratureDemodulation(source, 1 / sensitivity);
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
