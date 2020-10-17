package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import ru.r2cloud.jradio.source.PlutoSdr;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Util;

public class DopplerCorrectedSource implements FloatInput {

	private final FloatInput input;

	public DopplerCorrectedSource(PredictOreKit predict, File rawIq, ObservationRequest req) throws IOException {
		Long totalBytes = Util.readTotalBytes(rawIq.toPath());
		if (totalBytes == null) {
			throw new IllegalArgumentException("unable to read total samples");
		}

		FloatInput source;
		switch (req.getSdrType()) {
		case RTLSDR:
			source = new RtlSdr(new GZIPInputStream(new FileInputStream(rawIq)), req.getInputSampleRate(), totalBytes / 2);
			break;
		case PLUTOSDR:
			source = new PlutoSdr(new GZIPInputStream(new FileInputStream(rawIq)), req.getInputSampleRate(), totalBytes / 4);
			break;
		default:
			throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
		}
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(new org.orekit.propagation.analytical.tle.TLE(req.getTle().getRaw()[1], req.getTle().getRaw()[2]));
		TopocentricFrame groundStation = predict.getPosition(req.getGroundStation());
		long startFrequency = predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getStartTimeMillis(), groundStation, tlePropagator);
		long endFrequency = predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getEndTimeMillis(), groundStation, tlePropagator);

		long maxOffset = Math.max(Math.abs(req.getSatelliteFrequency() - startFrequency), Math.abs(req.getSatelliteFrequency() - endFrequency));

		long finalBandwidth = maxOffset + req.getBandwidth() / 2;

		float[] taps = Firdes.lowPass(1.0, source.getContext().getSampleRate(), finalBandwidth, 1600, Window.WIN_HAMMING, 6.76);
		FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(source, taps, req.getInputSampleRate() / req.getOutputSampleRate(), (double) req.getSatelliteFrequency() - req.getActualFrequency());
		SigSource source2 = new SigSource(Waveform.COMPLEX, (long) xlating.getContext().getSampleRate(), new DopplerValueSource(xlating.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

			@Override
			public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
				return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, groundStation, tlePropagator);
			}
		}, 1.0);
		input = new Multiply(xlating, source2);
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
