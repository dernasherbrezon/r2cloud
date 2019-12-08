package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.lrpt.LRPTInputStream;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.jradio.meteor.MeteorM;
import ru.r2cloud.jradio.meteor.MeteorMN22;
import ru.r2cloud.jradio.sink.WavFileSink;
import ru.r2cloud.jradio.source.InputStreamSource;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class MeteorM22Decoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(MeteorM22Decoder.class);

	private final Configuration config;
	private final ProcessFactory factory;

	public MeteorM22Decoder(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public ObservationResult decode(File rawIq, ObservationRequest req) {
		ObservationResult result = new ObservationResult();
		result.setIqPath(rawIq);

		// 1. correct doppler
		File dopplerCorrected = new File(config.getTempDirectory(), "meteor-m22.wav");
		WavFileSink tempWav = null;
		try {
			DopplerCorrectedSource source = new DopplerCorrectedSource(rawIq, req);
			tempWav = new WavFileSink(source, 16);
			try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(dopplerCorrected))) {
				tempWav.process(fos);
			}
		} catch (IOException e) {
			LOG.error("unable to correct doppler", e);
			return result;
		} finally {
			Util.closeQuietly(tempWav);
		}

		// 2. demodulate oqpsk
		int symbolRate = 72000;
		ProcessWrapper process = null;
		File softSymbols = new File(config.getTempDirectory(), "meteor-m22.s");
		try {
			process = factory.create(config.getProperty("satellites.meteor_demod.path") + " --quiet --output " + softSymbols.getAbsolutePath() + " --mode oqpsk --symrate " + symbolRate + " " + dopplerCorrected.getAbsolutePath(), false, false);
			int responseCode = process.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code meteor_demod: {}", req.getId(), responseCode);
				if (softSymbols.exists() && !softSymbols.delete()) {
					LOG.error("[{}] unable to delete temp file: {}", req.getId(), softSymbols.getAbsolutePath());
				}
				return result;
			} else {
				LOG.info("[{}] meteor_demod stopped: {}", req.getId(), responseCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Util.shutdown("meteor_demod", process, 10000);
		} catch (IOException e) {
			LOG.error("unable to run", e);
			return result;
		} finally {
			deleteTempFile(dopplerCorrected);
		}

		// 3. decode data
		long numberOfDecodedPackets = 0;
		File binFile = new File(config.getTempDirectory(), "meteor-m22.bin");
		MeteorM lrpt = null;
		try {
			lrpt = new MeteorMN22(new InputStreamSource(new BufferedInputStream(new FileInputStream(softSymbols))), symbolRate);
			try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(binFile))) {
				while (lrpt.hasNext()) {
					Vcdu next = lrpt.next();
					fos.write(next.getData());
					numberOfDecodedPackets++;
				}
			}
		} catch (IOException e) {
			LOG.error("unable to decode", e);
		} finally {
			Util.closeQuietly(lrpt);
			deleteTempFile(softSymbols);
		}

		result.setNumberOfDecodedPackets(numberOfDecodedPackets);
		if (numberOfDecodedPackets <= 0) {
			deleteTempFile(binFile);
		} else {
			result.setDataPath(binFile);
			try (LRPTInputStream lrptFile = new LRPTInputStream(new FileInputStream(binFile))) {
				MeteorImage image = new MeteorImage(lrptFile);
				BufferedImage actual = image.toBufferedImage();
				if (actual != null) {
					File imageFile = new File(config.getTempDirectory(), "lrpt-" + req.getId() + ".jpg");
					ImageIO.write(actual, "jpg", imageFile);
					if (req.getStartLatitude() < req.getEndLatitude()) {
						Util.rotateImage(imageFile);
					}
					result.setaPath(imageFile);
				}
			} catch (IOException e) {
				LOG.error("unable to generate image", e);
			}
		}

		return result;
	}

	private static void deleteTempFile(File binFile) {
		if (binFile.exists() && !binFile.delete()) {
			LOG.error("unable to delete temp file: {}", binFile.getAbsolutePath());
		}
	}
	
}
