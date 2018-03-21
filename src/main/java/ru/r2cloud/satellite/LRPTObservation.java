package ru.r2cloud.satellite;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.BufferedByteInput;
import ru.r2cloud.jradio.Context;
import ru.r2cloud.jradio.blocks.AGC;
import ru.r2cloud.jradio.blocks.ClockRecoveryMMComplex;
import ru.r2cloud.jradio.blocks.Constellation;
import ru.r2cloud.jradio.blocks.ConstellationSoftDecoder;
import ru.r2cloud.jradio.blocks.CorrelateAccessCodeTag;
import ru.r2cloud.jradio.blocks.CostasLoop;
import ru.r2cloud.jradio.blocks.FixedLengthTagger;
import ru.r2cloud.jradio.blocks.FloatToChar;
import ru.r2cloud.jradio.blocks.LowPassFilter;
import ru.r2cloud.jradio.blocks.Rail;
import ru.r2cloud.jradio.blocks.RootRaisedCosineFilter;
import ru.r2cloud.jradio.blocks.TaggedStreamToPdu;
import ru.r2cloud.jradio.blocks.Window;
import ru.r2cloud.jradio.lrpt.LRPT;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class LRPTObservation implements Observation {

	private static final Logger LOG = LoggerFactory.getLogger(LRPTObservation.class);
	private static final float INPUT_SAMPLE_RATE = 226000.0f;
	private static final float OUTPUT_SAMPLE_RATE = 135000.0f;
	private static final int BUF_SIZE = 0x1000; // 4K

	private ProcessWrapper rtlSdr = null;
	private File wavPath;

	private final Satellite satellite;
	private final Configuration config;
	private final SatPass nextPass;
	private final ProcessFactory factory;
	private final SatelliteDao dao;

	public LRPTObservation(Configuration config, Satellite satellite, SatPass nextPass, ProcessFactory factory, SatelliteDao dao) {
		this.config = config;
		this.satellite = satellite;
		this.nextPass = nextPass;
		this.factory = factory;
		this.dao = dao;
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
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r " + INPUT_SAMPLE_RATE + " -es -b 8 --channels 2 - " + wavPath.getAbsolutePath() + " rate " + OUTPUT_SAMPLE_RATE, Redirect.INHERIT, false);
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + String.valueOf(satellite.getFrequency()) + " -s " + INPUT_SAMPLE_RATE + " -g 45 -p " + String.valueOf(ppm) + " - ", Redirect.INHERIT, false);
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = rtlSdr.getInputStream().read(buf);
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
			Util.shutdown("rtl_sdr for satellites", rtlSdr, 10000);
			Util.shutdown("sox", sox, 10000);
		}
	}

	@Override
	public void stop() {
		Util.shutdown("rtl_sdr for satellites", rtlSdr, 10000);
		rtlSdr = null;

		if (!wavPath.exists()) {
			LOG.info("nothing saved");
			return;
		}

		String observationId = String.valueOf(nextPass.getStart().getTime().getTime());

		if (!dao.createObservation(satellite.getId(), observationId, wavPath)) {
			return;
		}

		ObservationResult cur = dao.find(satellite.getId(), observationId);
		if (cur == null) {
			return;
		}

		float symbolRate = 72000f;
		float clockAlpha = 0.010f;
		LOG.info("started");
		LRPT lrpt = null;
		try {
			WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream(wavPath)));
			LowPassFilter lowPass = new LowPassFilter(source, 1.0, OUTPUT_SAMPLE_RATE, 50000.0, 1000.0, Window.WIN_HAMMING, 6.76);
			AGC agc = new AGC(lowPass, 1000e-4f, 0.5f, 1.0f, 4000.0f);
			RootRaisedCosineFilter rrcf = new RootRaisedCosineFilter(agc, 1.0f, OUTPUT_SAMPLE_RATE, symbolRate, 0.6f, 361);

			CostasLoop costas = new CostasLoop(rrcf, 0.020f, 4, false);
			float omega = (float) ((OUTPUT_SAMPLE_RATE * 1.0) / (symbolRate * 1.0));
			ClockRecoveryMMComplex clockmm = new ClockRecoveryMMComplex(costas, omega, clockAlpha * clockAlpha / 4, 0.5f, clockAlpha, 0.005f);
			Constellation constel = new Constellation(new float[] { -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f }, new int[] { 0, 1, 3, 2 }, 4, 1);
			ConstellationSoftDecoder constelDecoder = new ConstellationSoftDecoder(clockmm, constel);
			Rail rail = new Rail(constelDecoder, -1.0f, 1.0f);
			FloatToChar f2char = new FloatToChar(rail, 127.0f);

			Set<String> accessCodes = new HashSet<>(LRPT.SYNCHRONIZATION_MARKERS.length);
			for (long curMarker : LRPT.SYNCHRONIZATION_MARKERS) {
				accessCodes.add(StringUtils.leftPad(Long.toBinaryString(curMarker), 64, '0'));
			}

			Context context = new Context();
			BufferedByteInput buffer = new BufferedByteInput(f2char, 8160 * 2, 8 * 2);
			CorrelateAccessCodeTag correlate = new CorrelateAccessCodeTag(context, buffer, 12, accessCodes, true);
			TaggedStreamToPdu tag = new TaggedStreamToPdu(context, new FixedLengthTagger(context, correlate, 8160 * 2 + 8 * 2));
			lrpt = new LRPT(context, tag, buffer);
			MeteorImage image = new MeteorImage(lrpt);
			LOG.info("decoded");
			BufferedImage actual = image.toBufferedImage();
			if (actual != null) {
				File imageFile = File.createTempFile(satellite.getId() + "-", ".jpg");
				ImageIO.write(actual, "jpg", imageFile);
				dao.saveChannel(satellite.getId(), observationId, imageFile, "a");
			}
		} catch (Exception e) {
			LOG.error("unable to process: " + wavPath.getAbsolutePath(), e);
		} finally {
			if (lrpt != null) {
				try {
					lrpt.close();
				} catch (IOException e) {
					LOG.info("unable to close", e);
				}
			}
		}

		cur.setStart(nextPass.getStart().getTime());
		cur.setEnd(nextPass.getEnd().getTime());
		dao.saveMeta(satellite.getId(), cur);
	}

	@Override
	public SatPass getNextPass() {
		return nextPass;
	}

}
