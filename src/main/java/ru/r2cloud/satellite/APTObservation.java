package ru.r2cloud.satellite;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Date;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.APTResult;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class APTObservation implements Observation {

	private static final Logger LOG = LoggerFactory.getLogger(APTObservation.class);
	private static final int BUF_SIZE = 0x1000; // 4K
	private static final int SAMPLE_RATE = 11025;

	private ProcessWrapper rtlfm = null;
	private File wavPath;

	private final Satellite satellite;
	private final Configuration config;
	private final SatPass nextPass;
	private final ProcessFactory factory;
	private final ObservationResultDao dao;
	private final APTDecoder aptDecoder;
	private final String observationId;

	public APTObservation(Configuration config, Satellite satellite, SatPass nextPass, ProcessFactory factory, ObservationResultDao dao, APTDecoder aptDecoder) {
		this.config = config;
		this.satellite = satellite;
		this.nextPass = nextPass;
		this.factory = factory;
		this.dao = dao;
		this.aptDecoder = aptDecoder;
		this.observationId = String.valueOf(nextPass.getStart().getTime().getTime());
	}

	@Override
	public void start() {
		try {
			this.wavPath = File.createTempFile(satellite.getId() + "-", ".wav");
		} catch (IOException e) {
			LOG.error("unable to create temp file", e);
			return;
		}
		ProcessWrapper sox = null;
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r 60000 -es -b 16 - " + wavPath.getAbsolutePath() + " rate " + SAMPLE_RATE, Redirect.INHERIT, false);
			rtlfm = factory.create(config.getProperty("satellites.rtlfm.path") + " -f " + String.valueOf(satellite.getFrequency()) + " -s 60k -g 45 -p " + String.valueOf(ppm) + " -E deemp -F 9 -", Redirect.INHERIT, false);
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = rtlfm.getInputStream().read(buf);
				if (r == -1) {
					break;
				}
				sox.getOutputStream().write(buf, 0, r);
			}
			sox.getOutputStream().flush();
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} finally {
			LOG.info("stopping pipe thread");
			Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
			Util.shutdown("sox", sox, 10000);
		}
	}

	@Override
	public void stop() {
		Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
		rtlfm = null;

		if (!wavPath.exists()) {
			LOG.info("nothing saved");
			return;
		}

		if (!dao.createObservation(satellite.getId(), observationId, wavPath)) {
			return;
		}
	}

	@Override
	public void decode() {
		ObservationResult cur = dao.find(satellite.getId(), observationId);
		if (cur == null) {
			return;
		}

		APTResult result = aptDecoder.decode(cur.getWavPath());
		if (result.getImage() != null) {
			// the image will be upside down if satellites pass 
			// from south to north
			if (nextPass.getStart().getLatitude() < nextPass.getEnd().getLatitude()) {
				rotateImage(result.getImage());
			}
			dao.saveChannel(satellite.getId(), observationId, result.getImage(), "a");
		}

		cur.setStart(nextPass.getStart().getTime());
		cur.setEnd(nextPass.getEnd().getTime());
		cur.setGain(result.getGain());
		cur.setChannelA(result.getChannelA());
		cur.setChannelB(result.getChannelB());
		cur.setSampleRate(SAMPLE_RATE);
		cur.setFrequency(satellite.getFrequency());
		dao.saveMeta(satellite.getId(), cur);
	}

	private static void rotateImage(File result) {
		try {
			BufferedImage image;
			try (FileInputStream fis = new FileInputStream(result)) {
				image = ImageIO.read(fis);
			}
			AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
			tx.translate(-image.getWidth(null), -image.getHeight(null));
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			image = op.filter(image, null);
			try (FileOutputStream fos = new FileOutputStream(result)) {
				ImageIO.write(image, "jpg", fos);
			}
		} catch (Exception e) {
			LOG.error("unable to rotate image", e);
		}
	}

	@Override
	public Date getStart() {
		return nextPass.getStart().getTime();
	}

	@Override
	public Date getEnd() {
		return nextPass.getEnd().getTime();
	}

	@Override
	public String getId() {
		return observationId;
	}

}
