package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class RtlSdrReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(RtlSdrReader.class);

	private ProcessWrapper rtlSdr = null;

	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;

	public RtlSdrReader(Configuration config, ProcessFactory factory, ObservationRequest req) {
		this.config = config;
		this.factory = factory;
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw.gz");
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		if (!startBiasT(config, factory, req.getId())) {
			return null;
		}
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			startTimeMillis = System.currentTimeMillis();
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdrwrapper.path") + " -rtl " + config.getProperty("satellites.rtlsdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g " + config.getProperty("satellites.rtlsdr.gain") + " -p " + ppm + " -o " + rawFile.getAbsolutePath(), Redirect.INHERIT, false);
			int responseCode = rtlSdr.waitFor();
			// rtl_sdr should be killed by the reaper process
			// all other codes are invalid. even 0
			if (responseCode != 143) {
				LOG.error("[{}] invalid response code rtl_sdr: {}", req.getId(), responseCode);
				Util.deleteQuietly(rawFile);
			} else {
				LOG.info("[{}] rtl_sdr stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to run", req.getId(), e);
		} finally {
			endTimeMillis = System.currentTimeMillis();
			stopBiasT(config, factory, req.getId());
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);

		if (rawFile.exists()) {
			result.setDataFile(rawFile);
		}
		return result;
	}

	static boolean startBiasT(Configuration config, ProcessFactory factory, String requestId) throws InterruptedException {
		boolean biast = config.getBoolean("satellites.rtlsdr.biast");
		if (!biast) {
			return true;
		}
		ProcessWrapper rtlBiast;
		try {
			rtlBiast = factory.create(config.getProperty("satellites.rtlsdr.biast.path") + " -b 1", Redirect.INHERIT, false);
			int responseCode = rtlBiast.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code rtl_biast: {}", requestId, responseCode);
				return false;
			}
			return true;
		} catch (IOException e) {
			LOG.error("[{}] unable to run rtl_biast", requestId, e);
			return false;
		}
	}

	static void stopBiasT(Configuration config, ProcessFactory factory, String requestId) throws InterruptedException {
		boolean biast = config.getBoolean("satellites.rtlsdr.biast");
		if (!biast) {
			return;
		}
		ProcessWrapper rtlBiast;
		try {
			rtlBiast = factory.create(config.getProperty("satellites.rtlsdr.biast.path") + " -b 0", Redirect.INHERIT, false);
			int responseCode = rtlBiast.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code rtl_biast: {}", requestId, responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to stop rtl_biast", requestId, e);
		}
	}

	@Override
	public void complete() {
		Util.shutdown("rtl_sdr for " + req.getId(), rtlSdr, 10000);
	}

}
