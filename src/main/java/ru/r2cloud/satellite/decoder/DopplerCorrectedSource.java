package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.analytical.tle.TLEPropagator;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.source.InputStreamSource;
import ru.r2cloud.jradio.source.PlutoSdr;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Util;

public class DopplerCorrectedSource implements FloatInput {

	private final FloatInput input;

	public DopplerCorrectedSource(PredictOreKit predict, File rawIq, ObservationRequest req, Transmitter transmitter) throws IOException {
		this(predict, rawIq, req, transmitter, false);
	}

	public DopplerCorrectedSource(PredictOreKit predict, File rawIq, ObservationRequest req, Transmitter transmitter, boolean snrAnalysis) throws IOException {
		Long totalBytes = Util.readTotalBytes(rawIq.toPath());
		if (totalBytes == null) {
			throw new IllegalArgumentException("unable to read total samples");
		}

		FloatInput next;
		InputStream is = new BufferedInputStream(new FileInputStream(rawIq));
		if (rawIq.toString().endsWith(".gz")) {
			is = new GZIPInputStream(is);
		}
		switch (req.getSdrType()) {
		case RTLSDR:
			next = new RtlSdr(is, req.getSampleRate(), totalBytes / 2);
			break;
		case PLUTOSDR:
			next = new PlutoSdr(is, req.getSampleRate(), totalBytes / 4);
			break;
		case SDRSERVER:
			Context ctx = new Context();
			ctx.setChannels(2);
			ctx.setSampleSizeInBits(4 * 8); // float = 4 bytes
			ctx.setSampleRate(req.getSampleRate());
			ctx.setTotalSamples(totalBytes / 8);
			next = new InputStreamSource(is, ctx);
			break;
		default:
			Util.closeQuietly(is);
			throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(req.getTle().getRaw()[1], req.getTle().getRaw()[2]));
		TopocentricFrame groundStation = predict.getPosition(req.getGroundStation());
		long startFrequency = predict.getDownlinkFreq(transmitter.getFrequency(), req.getStartTimeMillis(), groundStation, tlePropagator);
		long endFrequency = predict.getDownlinkFreq(transmitter.getFrequency(), req.getEndTimeMillis(), groundStation, tlePropagator);

		long maxOffset = Math.max(Math.abs(transmitter.getFrequency() - startFrequency), Math.abs(transmitter.getFrequency() - endFrequency));

		int decimation = req.getSampleRate() / transmitter.getOutputSampleRate();
		if (snrAnalysis) {
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) next.getContext().getSampleRate(), new DopplerValueSource(next.getContext().getSampleRate(), transmitter.getFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, groundStation, tlePropagator);
				}
			}, 1.0);
			next = new Multiply(next, source2);
			float[] taps = Firdes.lowPass(1.0, next.getContext().getSampleRate(), transmitter.getBandwidth(), 1600, Window.WIN_HAMMING, 6.76);
			input = new FrequencyXlatingFIRFilter(next, taps, decimation, (double) transmitter.getFrequency() - req.getActualFrequency());
		} else {
			float[] taps = Firdes.lowPass(1.0, next.getContext().getSampleRate(), maxOffset + (double) transmitter.getBandwidth() / 2, 1600, Window.WIN_HAMMING, 6.76);
			next = new FrequencyXlatingFIRFilter(next, taps, decimation, (double) transmitter.getFrequency() - req.getActualFrequency());
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) next.getContext().getSampleRate(), new DopplerValueSource(next.getContext().getSampleRate(), transmitter.getFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, groundStation, tlePropagator);
				}
			}, 1.0);
			input = new Multiply(next, source2);
		}

	}

	@Override
	public void close() throws IOException {
		if (input != null) {
			input.close();
		}
	}

	@Override
	public float readFloat() throws IOException {
		return input.readFloat();
	}

	@Override
	public Context getContext() {
		return input.getContext();
	}

}
