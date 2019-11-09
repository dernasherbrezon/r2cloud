package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;

public class DopplerCorrectedSource implements FloatInput {

	private final FloatInput input;

	public DopplerCorrectedSource(File rawIq, ObservationRequest req) throws IOException {
		Long totalSamples = Util.readTotalSamples(rawIq.toPath());
		if (totalSamples == null) {
			throw new IllegalArgumentException("unable to read total samples");
		}

		RtlSdr sdr = new RtlSdr(new GZIPInputStream(new FileInputStream(rawIq)), req.getInputSampleRate(), totalSamples);
		Satellite satellite = SatelliteFactory.createSatellite(req.getTle());
		long startFrequency = Predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getStartTimeMillis(), req.getGroundStation(), satellite);
		long endFrequency = Predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getEndTimeMillis(), req.getGroundStation(), satellite);
		
		long maxOffset = Math.max(Math.abs(req.getSatelliteFrequency() - startFrequency), Math.abs(req.getSatelliteFrequency() - endFrequency));
		
		long finalBandwidth = maxOffset + req.getBandwidth() / 2;

		float[] taps = Firdes.lowPass(1.0, sdr.getContext().getSampleRate(), finalBandwidth, 1600, Window.WIN_HAMMING, 6.76);
		FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(sdr, taps, req.getInputSampleRate() / req.getOutputSampleRate(), (double) req.getSatelliteFrequency() - req.getActualFrequency());
		SigSource source2 = new SigSource(Waveform.COMPLEX, (long) xlating.getContext().getSampleRate(), new DopplerValueSource(xlating.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

			@Override
			public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
				return Predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getGroundStation(), satellite);
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
