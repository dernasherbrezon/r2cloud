package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.aausat4.AAUSAT4;
import ru.r2cloud.jradio.aausat4.AAUSAT4Beacon;
import ru.r2cloud.jradio.blocks.ClockRecoveryMM;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Decoder;
import ru.r2cloud.satellite.Predict;

public class Aausat4Decoder implements Decoder {

	private final static Logger LOG = LoggerFactory.getLogger(Aausat4Decoder.class);
	private final Predict predict;

	public Aausat4Decoder(Predict predict) {
		this.predict = predict;
	}

	@Override
	public ObservationResult decode(File wavFile, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setWavPath(wavFile);
		long numberOfDecodedPackets = 0;
		File binFile;
		try {
			binFile = File.createTempFile("aausat4", ".bin");
		} catch (IOException e1) {
			LOG.error("unable to create temp file", e1);
			return result;
		}

		AAUSAT4 input = null;
		try {
			WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream(wavFile)));
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) source.getContext().getSampleRate(), new DopplerValueSource(source.getContext().getSampleRate(), req.getActualFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getOrigin());
				}
			}, 1.0);
			Multiply mul = new Multiply(source, source2, true);
			ClockRecoveryMM clockRecovery = new ClockRecoveryMM(mul, 20.0f, (float) (0.25 * 0.175 * 0.175), 0.005f, 0.175f, 0.005f);
			Rail rail = new Rail(clockRecovery, -1.0f, 1.0f);
			FloatToChar f2char = new FloatToChar(rail, 127.0f);
			CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(f2char, 10, "010011110101101000110100010000110101010101000010", true);
			input = new AAUSAT4(new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, AAUSAT4.VITERBI_TAIL_SIZE + 8))); // 8 for fsm
			try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(binFile))) {
				while (input.hasNext()) {
					AAUSAT4Beacon next = input.next();
					fos.write(next.getData());
					numberOfDecodedPackets++;
				}
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + wavFile, e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					LOG.info("unable to close", e);
				}
			}
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			if (!binFile.delete()) {
				LOG.error("unable to delete temp file: {}", binFile.getAbsolutePath());
			}
		} else {
			result.setDataPath(binFile);
		}
		return result;
	}

}
