package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.blocks.Constellation;
import ru.r2cloud.jradio.demod.QpskDemodulator;
import ru.r2cloud.jradio.lrpt.LRPTInputStream;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.jradio.meteor.MeteorM;
import ru.r2cloud.jradio.meteor.MeteorMN2;
import ru.r2cloud.model.DecoderResult;
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
		result.setRawPath(rawIq);

		int numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".bin");
		try {
			DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate);
			Constellation constel = new Constellation(new float[] { -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f }, new int[] { 0, 1, 3, 2 }, 4, 1);
			QpskDemodulator qpskDemod = new QpskDemodulator(source, baudRate, constel);
			lrpt = new MeteorMN2(qpskDemod);
			try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(binFile))) {
				while (lrpt.hasNext()) {
					Vcdu next = lrpt.next();
					fos.write(next.getData());
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
			result.setDataPath(binFile);
			try (LRPTInputStream lrptFile = new LRPTInputStream(new FileInputStream(binFile))) {
				MeteorImage image = new MeteorImage(lrptFile);
				BufferedImage actual = image.toBufferedImage();
				if (actual != null) {
					File imageFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".jpg");
					ImageIO.write(actual, "jpg", imageFile);
					result.setImagePath(imageFile);
				}
			} catch (IOException e) {
				LOG.error("unable to generate image", e);
			}
		}
		return result;
	}

}
