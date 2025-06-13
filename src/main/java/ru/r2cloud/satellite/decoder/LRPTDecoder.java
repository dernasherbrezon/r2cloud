package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.blocks.Constellation;
import ru.r2cloud.jradio.demod.QpskDemodulator;
import ru.r2cloud.jradio.lrpt.PacketReassembly;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.jradio.meteor.MeteorM;
import ru.r2cloud.jradio.meteor.MeteorMN2;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class LRPTDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(LRPTDecoder.class);

	private final Configuration config;
	private final PredictOreKit predict;

	public LRPTDecoder(PredictOreKit predict, Configuration config) {
		this.config = config;
		this.predict = predict;
	}

	@Override
	public DecoderResult decode(final File rawIq, final Observation req, final Transmitter transmitter, final Satellite satellite) {
		int baudRate = transmitter.getBaudRates().get(0);
		MeteorM lrpt = null;
		DecoderResult result = new DecoderResult();
		result.setIq(rawIq);

		int numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".bin");
		try {
			DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate);
			Constellation constel = new Constellation(new float[] { -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f }, new int[] { 0, 1, 3, 2 }, 4, 1);
			QpskDemodulator qpskDemod = new QpskDemodulator(source, baudRate, constel);
			lrpt = new MeteorMN2(qpskDemod);
			try (BeaconOutputStream fos = new BeaconOutputStream(new BufferedOutputStream(new FileOutputStream(binFile)))) {
				while (lrpt.hasNext()) {
					fos.write(lrpt.next());
					numberOfDecodedPackets++;
				}
			}
		} catch (Exception e) {
			LOG.error("unable to process: {}", rawIq.getAbsolutePath(), e);
		} finally {
			Util.closeQuietly(lrpt);
		}
		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			Util.deleteQuietly(binFile);
		} else {
			result.setData(binFile);
			Instrument msumr = setupMsumr(satellite, binFile);
			if (msumr != null) {
				result.setInstruments(Collections.singletonList(msumr));
			}
		}
		return result;
	}

	private Instrument setupMsumr(final Satellite satellite, File binFile) {
		MeteorImage meteorImage = null;
		try (BeaconInputStream<Vcdu> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(binFile)), Vcdu.class)) {
			meteorImage = new MeteorImage(new PacketReassembly(bis));
		} catch (IOException e) {
			LOG.error("unable to generate image", e);
			return null;
		}
		Instrument msumr = satellite.findById("MSUMR");
		if (msumr == null) {
			return null;
		}
		Instrument result = new Instrument(msumr);
		List<InstrumentChannel> channels = new ArrayList<>();
		for (InstrumentChannel cur : msumr.getChannels()) {
			// start with "1"
			int apid = Integer.valueOf(cur.getId());
			File image = saveImage(meteorImage.toBufferedImage(63 + apid), cur.getId());
			if (image == null) {
				continue;
			}
			InstrumentChannel copy = new InstrumentChannel(cur);
			copy.setImage(image);
			channels.add(copy);
		}
		if (channels.isEmpty()) {
			return null;
		}
		result.setChannels(channels);
		result.setCombinedImage(saveImage(meteorImage.toBufferedImage(65, 65, 64), "combined"));
		return result;
	}

	private File saveImage(BufferedImage image, String prefix) {
		if (image == null) {
			return null;
		}
		try {
			File imageFile = new File(config.getTempDirectory(), prefix + ".jpg");
			ImageIO.write(image, "jpg", imageFile);
			return imageFile;
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to save image", e);
			return null;
		}
	}

}
