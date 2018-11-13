package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.aausat4.AAUSAT4;
import ru.r2cloud.jradio.aausat4.AAUSAT4Beacon;
import ru.r2cloud.jradio.aausat4.AAUSAT4OutputStream;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.Firdes;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.FrequencyXlatingFIRFilter;
import ru.r2cloud.jradio.blocks.LowPassFilter;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.MultiplyConst;
import ru.r2cloud.jradio.blocks.QuadratureDemodulation;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.detection.GmskFrequencyCorrection;
import ru.r2cloud.jradio.detection.PeakDetection;
import ru.r2cloud.jradio.detection.PeakInterval;
import ru.r2cloud.jradio.detection.PeakValueSource;
import ru.r2cloud.jradio.sink.WavFileSink;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;

public class Aausat4Decoder implements Decoder {

	private final static Logger LOG = LoggerFactory.getLogger(Aausat4Decoder.class);

	private final Configuration config;
	private final Predict predict;

	public Aausat4Decoder(Configuration config, Predict predict) {
		this.config = config;
		this.predict = predict;
	}

	@Override
	public ObservationResult decode(File wavFile, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setWavPath(wavFile);
		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "aausat4-" + req.getId() + ".bin");
		File tempFile = new File(config.getTempDirectory(), "aausat4-" + req.getId() + ".temp");
		WavFileSink tempWav = null;
		BufferedOutputStream fos = null;
		try {
			// 1 stage. correct doppler & remove DC offset
			WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream(wavFile)));
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) source.getContext().getSampleRate(), new DopplerValueSource(source.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getOrigin());
				}
			}, 1.0);
			Multiply mul = new Multiply(source, source2, true);
			float[] taps = Firdes.lowPass(1.0, mul.getContext().getSampleRate(), 5000, 1000, Window.WIN_HAMMING, 6.76);
			FrequencyXlatingFIRFilter filter = new FrequencyXlatingFIRFilter(mul, taps, 5, -(req.getActualFrequency() - req.getSatelliteFrequency()));
			tempWav = new WavFileSink(filter, 16);
			fos = new BufferedOutputStream(new FileOutputStream(tempFile));
			tempWav.process(fos);
		} catch (Exception e) {
			LOG.error("unable to correct doppler: " + wavFile, e);
			return result;
		} finally {
			closeQuietly(tempWav);
			closeQuietly(fos);
		}
		// 2 stage. detect peaks
		List<PeakInterval> peaks;
		WavFileSource source = null;
		try {
			source = new WavFileSource(new BufferedInputStream(new FileInputStream(tempFile)));
			PeakDetection detection = new PeakDetection(10, -80.0f, 3);
			peaks = detection.process(source);
		} catch (Exception e) {
			LOG.error("unable to detect peaks: " + tempFile.getAbsolutePath(), e);
			return result;
		} finally {
			closeQuietly(source);
		}
		// 3 stage. correct peaks and decode
		AAUSAT4 input = null;
		AAUSAT4OutputStream aos = null;
		try {
			source = new WavFileSource(new BufferedInputStream(new FileInputStream(tempFile)));
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) source.getContext().getSampleRate(), new PeakValueSource(peaks, new GmskFrequencyCorrection(2400, 10)), 1.0f);
			Multiply mul = new Multiply(source, source2, true);
			QuadratureDemodulation qd = new QuadratureDemodulation(mul, 0.4f);
			LowPassFilter lpf = new LowPassFilter(qd, 1.0, 1500.0f, 100, Window.WIN_HAMMING, 6.76);
			MultiplyConst mc = new MultiplyConst(lpf, 1.0f);
			ClockRecoveryMM clockRecovery = new ClockRecoveryMM(mc, mc.getContext().getSampleRate() / 2400, (float) (0.25 * 0.175 * 0.175), 0.005f, 0.175f, 0.005f);
			Rail rail = new Rail(clockRecovery, -1.0f, 1.0f);
			FloatToChar f2char = new FloatToChar(rail, 127.0f);
			CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(f2char, 10, "010011110101101000110100010000110101010101000010", true);
			input = new AAUSAT4(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, AAUSAT4.VITERBI_TAIL_SIZE + 8))); // 8 for fsm
			aos = new AAUSAT4OutputStream(new FileOutputStream(binFile));
			while (input.hasNext()) {
				AAUSAT4Beacon next = input.next();
				aos.write(next);
				numberOfDecodedPackets++;
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + wavFile, e);
			return result;
		} finally {
			closeQuietly(input);
			closeQuietly(aos);
			if (!tempFile.delete()) {
				LOG.error("unable to delete temp file: " + tempFile.getAbsolutePath());
			}
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

	private static void closeQuietly(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			LOG.info("unable to close", e);
		}
	}

}
