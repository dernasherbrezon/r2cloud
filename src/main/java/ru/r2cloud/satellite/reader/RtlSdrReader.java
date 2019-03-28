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
	private File rawFile;
	private Long startTimeMillis = null;
	private Long endTimeMillis = null;

	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;

	public RtlSdrReader(Configuration config, ProcessFactory factory, ObservationRequest req) {
		this.config = config;
		this.factory = factory;
		this.req = req;
	}

	@Override
	public void start() {
		this.rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw");
		try {
			Integer ppm = config.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			if (startTimeMillis == null) {
				startTimeMillis = System.currentTimeMillis();
			}
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g 45 -p " + ppm + " " + rawFile.getAbsolutePath(), Redirect.INHERIT, true);
			int responseCode = rtlSdr.waitFor();
			LOG.info("rtl_sdr stopped: {}", responseCode);
		} catch (IOException e) {
			LOG.error("unable to run", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			LOG.info("stopping pipe thread");
			Util.shutdown("rtl_sdr for satellites", rtlSdr, 10000);
			endTimeMillis = System.currentTimeMillis();
		}
	}

	@Override
	public IQData complete() {
		Util.shutdown("rtl_sdr for satellites", rtlSdr, 10000);
		rtlSdr = null;

		IQData result = new IQData();

		if (rawFile != null && rawFile.exists()) {
			ProcessWrapper sox = null;
			File wavPath = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".wav");
			try {
				sox = factory.create(config.getProperty("satellites.sox.path") + " --no-dither --type raw --rate " + req.getInputSampleRate() + " --encoding unsigned-integer --bits 8 --channels 2 " + rawFile.getAbsolutePath() + " " + wavPath.getAbsolutePath() + " rate " + req.getOutputSampleRate(), Redirect.INHERIT, false);
				int responseCode = sox.waitFor();
				LOG.info("sox stopped: {}", responseCode);
			} catch (IOException e) {
				LOG.error("unable to run", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				Util.shutdown("sox", sox, 10000);
				if (!rawFile.delete()) {
					LOG.error("unable to delete temp file: {}", rawFile.getAbsolutePath());
				}
			}
			if (wavPath.exists()) {
				result.setWavFile(wavPath);
			}
		}
		if (startTimeMillis != null) {
			result.setActualStart(startTimeMillis);
		} else {
			// just to be on the safe side
			result.setActualStart(req.getStartTimeMillis());
		}
		if (endTimeMillis != null) {
			result.setActualEnd(endTimeMillis);
		} else {
			result.setActualEnd(req.getEndTimeMillis());
		}

		return result;
	}

}
