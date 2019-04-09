package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.PhaseAmbiguityResolver;
import ru.r2cloud.jradio.blocks.AGC;
import ru.r2cloud.jradio.blocks.ClockRecoveryMMComplex;
import ru.r2cloud.jradio.blocks.Constellation;
import ru.r2cloud.jradio.blocks.ConstellationSoftDecoder;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.CostasLoop;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.RootRaisedCosineFilter;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.lrpt.LRPT;
import ru.r2cloud.jradio.lrpt.LRPTInputStream;
import ru.r2cloud.jradio.lrpt.VCDU;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.jradio.source.RtlSdr;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class LRPTDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(LRPTDecoder.class);

	private final Predict predict;
	private final Configuration config;

	public LRPTDecoder(Configuration config, Predict predict) {
		this.config = config;
		this.predict = predict;
	}

	@Override
	public ObservationResult decode(final File rawIq, final ObservationRequest req) {
		float symbolRate = 72000f;
		float clockAlpha = 0.010f;
		LRPT lrpt = null;
		ObservationResult result = new ObservationResult();
		result.setIqPath(rawIq);
		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".bin");
		try {
			Long totalSamples = Util.readTotalSamples(rawIq);
			if (totalSamples != null) {
				RtlSdr sdr = new RtlSdr(new GZIPInputStream(new FileInputStream(rawIq)), req.getInputSampleRate(), totalSamples);
				float[] taps = Firdes.lowPass(1.0, sdr.getContext().getSampleRate(), sdr.getContext().getSampleRate() / 2, 1600, Window.WIN_HAMMING, 6.76);
				FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(sdr, taps, req.getInputSampleRate() / req.getOutputSampleRate(), req.getSatelliteFrequency() - req.getActualFrequency());
				SigSource source2 = new SigSource(Waveform.COMPLEX, (long) xlating.getContext().getSampleRate(), new DopplerValueSource(xlating.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

					@Override
					public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
						return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getOrigin());
					}
				}, 1.0);
				Multiply mul = new Multiply(xlating, source2);
				AGC agc = new AGC(mul, 1000e-4f, 0.5f, 2.0f, 4000.0f);
				RootRaisedCosineFilter rrcf = new RootRaisedCosineFilter(agc, 1.0f, symbolRate, 0.6f, 361);
				float omega = (float) ((xlating.getContext().getSampleRate() * 1.0) / (symbolRate * 1.0));
				ClockRecoveryMMComplex clockmm = new ClockRecoveryMMComplex(rrcf, omega, clockAlpha * clockAlpha / 4, 0.5f, clockAlpha, 0.005f);
				CostasLoop costas = new CostasLoop(clockmm, 0.008f, 4, false);
				Constellation constel = new Constellation(new float[] { -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f }, new int[] { 0, 1, 3, 2 }, 4, 1);
				ConstellationSoftDecoder constelDecoder = new ConstellationSoftDecoder(costas, constel);
				Rail rail = new Rail(constelDecoder, -1.0f, 1.0f);
				FloatToChar f2char = new FloatToChar(rail, 127.0f);

				PhaseAmbiguityResolver phaseAmbiguityResolver = new PhaseAmbiguityResolver(0x035d49c24ff2686bL);

				CorrelateAccessCodeTag correlate = new CorrelateAccessCodeTag(f2char, 12, phaseAmbiguityResolver.getSynchronizationMarkers(), true);
				TaggedStreamToPdu tag = new TaggedStreamToPdu(new FixedLengthTagger(correlate, 8160 * 2 + 8 * 2));
				lrpt = new LRPT(tag, phaseAmbiguityResolver, MeteorImage.METEOR_SPACECRAFT_ID);
				try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(binFile))) {
					while (lrpt.hasNext()) {
						VCDU next = lrpt.next();
						fos.write(next.getData());
						numberOfDecodedPackets++;
					}
				}
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + rawIq.getAbsolutePath(), e);
		} finally {
			if (lrpt != null) {
				try {
					lrpt.close();
				} catch (IOException e) {
					LOG.info("unable to close", e);
				}
			}
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			if (binFile.exists() && !binFile.delete()) {
				LOG.error("unable to delete temp file: {}", binFile.getAbsolutePath());
			}
		} else {
			result.setDataPath(binFile);
			try (LRPTInputStream lrptFile = new LRPTInputStream(new FileInputStream(binFile))) {
				MeteorImage image = new MeteorImage(lrptFile);
				BufferedImage actual = image.toBufferedImage();
				if (actual != null) {
					File imageFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".jpg");
					ImageIO.write(actual, "jpg", imageFile);
					result.setaPath(imageFile);
				}
			} catch (IOException e) {
				LOG.error("unable to generate image", e);
			}
		}
		return result;
	}

}
