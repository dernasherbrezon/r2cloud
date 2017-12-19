package ru.r2cloud.satellite;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.model.APTResult;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class Observation {

	private static final Logger LOG = LoggerFactory.getLogger(Observation.class);
	private static final int BUF_SIZE = 0x1000; // 4K

	private ProcessWrapper rtlfm = null;
	private File wavPath;

	private final Satellite satellite;
	private final Configuration config;
	private final SatPass nextPass;
	private final ProcessFactory factory;
	private final SatelliteDao dao;
	private final APTDecoder aptDecoder;

	public Observation(Configuration config, Satellite satellite, SatPass nextPass, ProcessFactory factory, SatelliteDao dao, APTDecoder aptDecoder) {
		this.config = config;
		this.satellite = satellite;
		this.nextPass = nextPass;
		this.factory = factory;
		this.dao = dao;
		this.aptDecoder = aptDecoder;
	}

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
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r 60000 -es -b 16 - " + wavPath.getAbsolutePath() + " rate 11025", Redirect.INHERIT, false);
			rtlfm = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + String.valueOf(satellite.getFrequency()) + " -s 60k -g 45 -p " + String.valueOf(ppm) + " -E deemp -F 9 -", Redirect.INHERIT, false);
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

	public void stop() {
		Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
		rtlfm = null;

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

		APTResult result = aptDecoder.decode(wavPath, "a");
		boolean decoded = false;
		if (result.getImage() != null) {
			if (dao.saveChannel(satellite.getId(), observationId, result.getImage(), "a")) {
				decoded = true;
			}
			// decode b channel only if a was successfully decoded
			result = aptDecoder.decode(wavPath, "b");
			if (result.getImage() != null) {
				dao.saveChannel(satellite.getId(), observationId, result.getImage(), "b");
			}
		}

		JsonObject meta = new JsonObject();
		meta.add("Start", nextPass.getStart().getTime().getTime());
		meta.add("End", nextPass.getEnd().getTime().getTime());
		if (result.getGain() != null) {
			meta.add("Gain", result.getGain());
		}
		if (result.getChannelA() != null) {
			meta.add("Channel A", result.getChannelA());
		}
		if (result.getChannelB() != null) {
			meta.add("Channel B", result.getChannelB());
		}
		meta.add("decoded", decoded);
		dao.saveMeta(satellite.getId(), observationId, meta);
	}

	public SatPass getNextPass() {
		return nextPass;
	}

}
