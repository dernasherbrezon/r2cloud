package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.BeaconSource;
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
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;

public abstract class TelemetryDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(TelemetryDecoder.class);

	private final Configuration config;

	public TelemetryDecoder(Configuration config) {
		this.config = config;
	}

	@Override
	public ObservationResult decode(File rawIq, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setIqPath(rawIq);

		Long totalSamples = Util.readTotalSamples(rawIq.toPath());
		if (totalSamples == null) {
			return result;
		}

		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), req.getId() + ".bin");
		BeaconSource<? extends Beacon> input = null;
		BeaconOutputStream aos = null;
		try {
			RtlSdr sdr = new RtlSdr(new GZIPInputStream(new FileInputStream(rawIq)), req.getInputSampleRate(), totalSamples);
			Satellite satellite = SatelliteFactory.createSatellite(req.getTle());
			long startOffset = Predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getStartTimeMillis(), req.getGroundStation(), satellite);
			long endOffset = Predict.getDownlinkFreq(req.getSatelliteFrequency(), req.getEndTimeMillis(), req.getGroundStation(), satellite);
			long finalBandwidth = startOffset - endOffset + req.getBandwidth() / 2;

			float[] taps = Firdes.lowPass(1.0, sdr.getContext().getSampleRate(), finalBandwidth, 1600, Window.WIN_HAMMING, 6.76);
			FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(sdr, taps, req.getInputSampleRate() / req.getOutputSampleRate(), (double) req.getSatelliteFrequency() - req.getActualFrequency());
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) xlating.getContext().getSampleRate(), new DopplerValueSource(xlating.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return Predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getGroundStation(), satellite);
				}
			}, 1.0);
			Multiply mul = new Multiply(xlating, source2);
			input = createBeaconSource(mul, req);
			aos = new BeaconOutputStream(new FileOutputStream(binFile));
			while (input.hasNext()) {
				Beacon next = input.next();
				next.setBeginMillis(req.getStartTimeMillis() + (long) ((next.getBeginSample() * 1000) / xlating.getContext().getSampleRate()));
				aos.write(next);
				numberOfDecodedPackets++;
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + rawIq, e);
			return result;
		} finally {
			Util.closeQuietly(input);
			Util.closeQuietly(aos);
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			if (binFile.exists() && !binFile.delete()) {
				LOG.error("unable to delete temp file: {}", binFile.getAbsolutePath());
			}
		} else {
			result.setDataPath(binFile);
		}
		return result;
	}

	public abstract BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req);

}
