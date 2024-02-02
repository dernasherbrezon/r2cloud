package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class RtlFmReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(RtlFmReader.class);
	private static final int BUF_SIZE = 0x1000; // 4K

	private ProcessWrapper rtlfm = null;

	private final DeviceConfiguration deviceConfiguration;
	private final ObservationRequest req;
	private final Configuration config;
	private final ProcessFactory factory;
	private final Object lock;

	public RtlFmReader(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req, Object lock) {
		this.config = config;
		this.deviceConfiguration = deviceConfiguration;
		this.factory = factory;
		this.req = req;
		this.lock = lock;
	}

	@Override
	public IQData start() throws InterruptedException {
		synchronized (lock) {
			return startInternally();
		}
	}

	private IQData startInternally() throws InterruptedException {
		File wavPath = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".wav");
		ProcessWrapper sox = null;
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		if (!RtlSdrReader.startBiasT(config, deviceConfiguration, factory, req)) {
			return null;
		}

		// RTL-FM is used only for APT. Thus harded sample rates
		int inputSampleRate = 60_000;
		int outputSampleRate = 11_025;

		try {
			sox = factory.create(config.getProperty("satellites.sox.path") + " -t raw -r " + inputSampleRate + " -es -b 16 - " + wavPath.getAbsolutePath() + " rate " + outputSampleRate, Redirect.INHERIT, false);
			rtlfm = factory.create(config.getProperty("satellites.rtlfm.path") + " -f " + req.getFrequency() + " -d " + deviceConfiguration.getRtlDeviceId() + " -s " + inputSampleRate + " -g " + deviceConfiguration.getGain() + " -p " + deviceConfiguration.getPpm() + " -E deemp -F 9 -",
					Redirect.INHERIT, false);
			byte[] buf = new byte[BUF_SIZE];
			while (!Thread.currentThread().isInterrupted()) {
				int r = rtlfm.getInputStream().read(buf);
				if (r == -1) {
					break;
				}
				if (startTimeMillis == null) {
					startTimeMillis = System.currentTimeMillis();
				}
				sox.getOutputStream().write(buf, 0, r);
			}
			sox.getOutputStream().flush();
			int responseCode = rtlfm.waitFor();
			if (responseCode != 143) {
				LOG.error("[{}] invalid response code rtl_fm: {}", req.getId(), responseCode);
				Util.deleteQuietly(wavPath);
			} else {
				LOG.info("[{}] rtl_fm stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			// there is no way to know if IOException was caused by closed stream
			if (e.getMessage() == null || !e.getMessage().equals("Stream closed")) {
				LOG.error("unable to run", e);
			}
		} finally {
			LOG.info("stopping pipe thread");
			Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
			Util.shutdown("sox", sox, 10000);
			endTimeMillis = System.currentTimeMillis();
			RtlSdrReader.stopBiasT(config, deviceConfiguration, factory, req);
		}

		IQData result = new IQData();

		if (wavPath.exists()) {
			result.setDataFile(wavPath);
		}
		if (startTimeMillis != null) {
			result.setActualStart(startTimeMillis);
		} else {
			// just to be on the safe side
			result.setActualStart(req.getStartTimeMillis());
		}
		result.setActualEnd(endTimeMillis);
		result.setDataFormat(DataFormat.UNKNOWN);
		// .wav file sampled at 11k after sox transformation
		result.setSampleRate(outputSampleRate);

		return result;
	}

	@Override
	public void complete() {
		Util.shutdown("rtl_sdr for satellites", rtlfm, 10000);
		rtlfm = null;
	}

}
