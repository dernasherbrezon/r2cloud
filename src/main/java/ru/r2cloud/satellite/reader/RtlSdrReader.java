package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class RtlSdrReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(RtlSdrReader.class);

	private ProcessWrapper rtlSdr = null;

	private final List<Long> supportedSampleRates = new ArrayList<>();
	private final DeviceConfiguration deviceConfiguration;
	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;
	private final Transmitter transmitter;
	private final Object lock;

	public RtlSdrReader(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req, Transmitter transmitter, Object lock) {
		this.config = config;
		this.deviceConfiguration = deviceConfiguration;
		this.factory = factory;
		this.req = req;
		this.transmitter = transmitter;
		this.lock = lock;
		for (int i = 1;; i++) {
			long rate = 28_800_000 / i;
			if (rate <= 225000) {
				break;
			}
			// in practice rtl-sdr can't archive more than 2.4MSPS
			if (rate > 2_400_000) {
				continue;
			}
			if (((rate > 300000) && (rate <= 900000))) {
				continue;
			}
			long remainder = 28_800_000 % i;
			// accept only integer rates
			if (remainder != 0) {
				continue;
			}
			supportedSampleRates.add(rate);
		}
	}

	@Override
	public IQData start() throws InterruptedException {
		synchronized (lock) {
			return startInternally();
		}
	}

	private IQData startInternally() throws InterruptedException {
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw.gz");
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		if (!startBiasT(config, deviceConfiguration, factory, req)) {
			return null;
		}

		Integer maxBaudRate = Collections.max(transmitter.getBaudRates());
		if (maxBaudRate == null) {
			return null;
		}

		Long sampleRate = Util.getSmallestGoodDeviceSampleRate(maxBaudRate, supportedSampleRates);
		if (sampleRate == null) {
			LOG.error("[{}] cannot find sample rate for: {}", req.getId(), maxBaudRate);
			return null;
		}
		try {
			startTimeMillis = System.currentTimeMillis();
			rtlSdr = factory.create(config.getProperty("satellites.rtlsdrwrapper.path") + " -rtl " + config.getProperty("satellites.rtlsdr.path") + " -f " + req.getFrequency() + " -d " + deviceConfiguration.getRtlDeviceId() + " -s " + sampleRate + " -g " + deviceConfiguration.getGain() + " -p "
					+ deviceConfiguration.getPpm() + " -o " + rawFile.getAbsolutePath(), Redirect.INHERIT, false);
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
			stopBiasT(config, deviceConfiguration, factory, req);
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFormat(DataFormat.COMPLEX_UNSIGNED_BYTE);
		result.setSampleRate(sampleRate);
		if (rawFile.exists()) {
			result.setDataFile(rawFile);
		}
		return result;
	}

	static boolean startBiasT(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req) throws InterruptedException {
		if (!deviceConfiguration.isBiast()) {
			return true;
		}
		ProcessWrapper rtlBiast;
		try {
			rtlBiast = factory.create(config.getProperty("satellites.rtlsdr.biast.path") + " -b 1", Redirect.INHERIT, false);
			int responseCode = rtlBiast.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code rtl_biast: {}", req.getId(), responseCode);
				return false;
			}
			return true;
		} catch (IOException e) {
			LOG.error("[{}] unable to run rtl_biast", req.getId(), e);
			return false;
		}
	}

	static void stopBiasT(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req) throws InterruptedException {
		if (!deviceConfiguration.isBiast()) {
			return;
		}
		ProcessWrapper rtlBiast;
		try {
			rtlBiast = factory.create(config.getProperty("satellites.rtlsdr.biast.path") + " -b 0", Redirect.INHERIT, false);
			int responseCode = rtlBiast.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code rtl_biast: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to stop rtl_biast", req.getId(), e);
		}
	}

	@Override
	public void complete() {
		Util.shutdown("rtl_sdr for " + req.getId(), rtlSdr, 10000);
	}

}
