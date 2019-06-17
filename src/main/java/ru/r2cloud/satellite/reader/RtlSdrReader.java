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
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			startTimeMillis = System.currentTimeMillis();
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdrwrapper.path") + " -rtl " + config.getProperty("satellites.rtlsdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g 45 -p " + ppm + " -o " + rawFile.getAbsolutePath(), Redirect.INHERIT, false);
			int responseCode = rtlSdr.waitFor();
			// rtl_sdr should be killed by the reaper process
			// all other codes are invalid. even 0
			if (responseCode != 143) {
				LOG.error("[{}] invalid response code rtl_sdr: {}", req.getId(), responseCode);
				if (rawFile.exists() && !rawFile.delete()) {
					LOG.error("[{}] unable to delete temp file: {}", req.getId(), rawFile.getAbsolutePath());
				}
			} else {
				LOG.info("[{}] rtl_sdr stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[" + req.getId() + "] unable to run", e);
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);

		if (rawFile.exists()) {
			result.setIqFile(rawFile);
		}
		return result;
	}

	@Override
	public void complete() {
		Util.shutdown("rtl_sdr for " + req.getId(), rtlSdr, 10000);
		rtlSdr = null;
	}

}
