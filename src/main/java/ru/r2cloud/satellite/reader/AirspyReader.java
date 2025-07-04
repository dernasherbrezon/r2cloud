package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class AirspyReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(AirspyReader.class);

	private ProcessWrapper process = null;

	private final List<Long> supportedSampleRates;
	private final DeviceConfiguration deviceConfiguration;
	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;
	private final Transmitter transmitter;
	private final ReentrantLock lock;

	public AirspyReader(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req, Transmitter transmitter, ReentrantLock lock, List<Long> supportedSampleRates) {
		this.config = config;
		this.deviceConfiguration = deviceConfiguration;
		this.factory = factory;
		this.req = req;
		this.transmitter = transmitter;
		this.lock = lock;
		this.supportedSampleRates = supportedSampleRates;
	}

	@Override
	public IQData start() throws InterruptedException {
		lock.lock();
		try {
			return startInternally();
		} finally {
			lock.unlock();
		}
	}

	private IQData startInternally() throws InterruptedException {
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		Long sampleRate = getSampleRate();
		if (sampleRate == null) {
			return null;
		}
		DataFormat dataFormat = DataFormat.COMPLEX_SIGNED_SHORT;
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + "." + dataFormat.getExtension());
		try {
			startTimeMillis = System.currentTimeMillis();
			StringBuilder command = new StringBuilder();
			command.append(config.getProperty("satellites.airspy_rx.path"));
			command.append(" -f " + (req.getFrequency() / 1000000.0));
			command.append(" -r " + rawFile.getAbsolutePath());
			if (deviceConfiguration.getRtlDeviceId() != null) {
				command.append(" -s " + deviceConfiguration.getRtlDeviceId());
			}
			command.append(" -a " + sampleRate);
			command.append(" -t 2"); // INT16_IQ
			if (deviceConfiguration.isBiast()) {
				command.append(" -b 1");
			}
			switch (deviceConfiguration.getGainType()) {
			case LINEAR: {
				command.append(" -g " + (int) deviceConfiguration.getGain());
				break;
			}
			case SENSITIVE: {
				command.append(" -h " + (int) deviceConfiguration.getGain());
				break;
			}
			case FREE: {
				command.append(" -v " + (int) deviceConfiguration.getVgaGain());
				command.append(" -m " + (int) deviceConfiguration.getMixerGain());
				command.append(" -l " + (int) deviceConfiguration.getLnaGain());
			}
			}

			process = factory.create(command.toString(), Redirect.INHERIT, false);
			int responseCode = process.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code airspy_rx: {}", req.getId(), responseCode);
				Util.deleteQuietly(rawFile);
			} else {
				LOG.info("[{}] airspy_rx stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to run", req.getId(), e);
		} finally {
			endTimeMillis = System.currentTimeMillis();
		}
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFormat(dataFormat);
		result.setSampleRate(sampleRate);
		if (rawFile.exists()) {
			result.setIq(rawFile);
		}
		return result;
	}

	@Override
	public void complete() {
		Util.shutdown("airspy_Rx for " + req.getId(), process, 10000);
	}

	private Long getSampleRate() {
		if (transmitter.getFraming() != null && transmitter.getFraming().equals(Framing.SATDUMP)) {
			// assume sorted asc
			for (Long cur : supportedSampleRates) {
				if (cur >= transmitter.getBandwidth()) {
					return cur;
				}
			}
			return null;
		}
		Integer maxBaudRate = Collections.max(transmitter.getBaudRates());
		if (maxBaudRate == null) {
			LOG.error("[{}] no configured baud raters", req.getId());
			return null;
		}
		Long sampleRate = Util.getSmallestGoodDeviceSampleRate(maxBaudRate, supportedSampleRates);
		if (sampleRate == null) {
			LOG.error("[{}] cannot find sample rate for: {}", req.getId(), maxBaudRate);
			return null;
		}
		return sampleRate;
	}

}
