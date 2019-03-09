package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.DopplerValueSource;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.Multiply;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.gomx1.AX100Decoder;
import ru.r2cloud.jradio.kunspf.KunsPf;
import ru.r2cloud.jradio.kunspf.KunsPfBeacon;
import ru.r2cloud.jradio.source.SigSource;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.jradio.source.Waveform;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class KunsPfDecoder implements Decoder {
	
	private static final Logger LOG = LoggerFactory.getLogger(KunsPfDecoder.class);

	private final Configuration config;
	private final Predict predict;
	
	public KunsPfDecoder(Configuration config, Predict predict) {
		this.config = config;
		this.predict = predict;
	}
	
	@Override
	public ObservationResult decode(File wavFile, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setWavPath(wavFile);
		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "kunspf-" + req.getId() + ".bin");
		KunsPf input = null;
		BeaconOutputStream aos = null;
		try {
			WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream(wavFile)));
			SigSource source2 = new SigSource(Waveform.COMPLEX, (long) source.getContext().getSampleRate(), new DopplerValueSource(source.getContext().getSampleRate(), req.getSatelliteFrequency(), 1000L, req.getStartTimeMillis()) {

				@Override
				public long getDopplerFrequency(long satelliteFrequency, long currentTimeMillis) {
					return predict.getDownlinkFreq(satelliteFrequency, currentTimeMillis, req.getOrigin());
				}
			}, 1.0);
			Multiply mul = new Multiply(source, source2);
			GmskDemodulator demodulator = new GmskDemodulator(mul, -(req.getActualFrequency() - req.getSatelliteFrequency()), 1200, 0.175f * 3);
			CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(demodulator, 4, "10010011000010110101000111011110", true);
			// 73 choosen as an estimated packet length in test.
			// in real prod, it better to have max - 255
			TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, 73 * 8));
			AX100Decoder ax100 = new AX100Decoder(pdu, false, true, true);
			input = new KunsPf(ax100);
			aos = new BeaconOutputStream(new FileOutputStream(binFile));
			while (input.hasNext()) {
				KunsPfBeacon next = input.next();
				next.setBeginMillis(req.getStartTimeMillis() + (long) ((next.getBeginSample() * 1000) / source.getContext().getSampleRate()));
				aos.write(next);
				numberOfDecodedPackets++;
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + wavFile, e);
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

}
