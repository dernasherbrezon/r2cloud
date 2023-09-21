package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Collections;

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

public class PlutoSdrReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(PlutoSdrReader.class);

	private ProcessWrapper plutoSdrCli = null;

	private final Configuration config;
	private final DeviceConfiguration deviceConfiguration;
	private final ProcessFactory factory;
	private final ObservationRequest req;
	private final Transmitter transmitter;

	public PlutoSdrReader(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req, Transmitter transmitter) {
		this.config = config;
		this.deviceConfiguration = deviceConfiguration;
		this.factory = factory;
		this.req = req;
		this.transmitter = transmitter;
	}

	@Override
	public IQData start() throws InterruptedException {
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw.gz");
		Long startTimeMillis = null;
		Long endTimeMillis = null;

		Integer maxBaudRate = Collections.max(transmitter.getBaudRates());
		if (maxBaudRate == null) {
			return null;
		}

		int inputSampleRate;
		if (maxBaudRate == 72_000) {
			inputSampleRate = 288_000;
		} else if (50_000 % maxBaudRate == 0) {
			inputSampleRate = 50_000 * 5;
		} else {
			inputSampleRate = 48_000 * 5;
		}

		try {
			startTimeMillis = System.currentTimeMillis();
			plutoSdrCli = factory.create(config.getProperty("satellites.plutosdr.wrapper.path") + " -cli " + config.getProperty("satellites.plutosdr.path") + " -f " + req.getFrequency() + " -s " + inputSampleRate + " -g " + deviceConfiguration.getGain() + " -o " + rawFile.getAbsolutePath(),
					Redirect.INHERIT, false);
			int responseCode = plutoSdrCli.waitFor();
			if (responseCode != 143) {
				LOG.error("[{}] invalid response code plutoSdrCli: {}", req.getId(), responseCode);
				Util.deleteQuietly(rawFile);
			} else {
				LOG.info("[{}] plutoSdrCli stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to run", req.getId(), e);
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFormat(DataFormat.COMPLEX_SIGNED_SHORT);
		result.setSampleRate(inputSampleRate);
		if (rawFile.exists()) {
			result.setDataFile(rawFile);
		}
		return result;
	}

	@Override
	public void complete() {
		Util.shutdown("plutoSdrCli for " + req.getId(), plutoSdrCli, 10000);
	}

}
