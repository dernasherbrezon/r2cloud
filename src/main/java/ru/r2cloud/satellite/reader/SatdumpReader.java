package ru.r2cloud.satellite.reader;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class SatdumpReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(SatdumpReader.class);

	private final Configuration config;
	private final DeviceConfiguration deviceConfiguration;
	private final ObservationRequest req;
	private final ProcessFactory factory;
	private final Transmitter transmitter;
	private final ReentrantLock lock;
	private ProcessWrapper satdump = null;

	public SatdumpReader(Configuration config, DeviceConfiguration deviceConfiguration, ProcessFactory factory, ObservationRequest req, Transmitter transmitter, ReentrantLock lock) {
		this.config = config;
		this.deviceConfiguration = deviceConfiguration;
		this.req = req;
		this.factory = factory;
		this.transmitter = transmitter;
		this.lock = lock;
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
		File outputDirectory = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId());
		Long startTimeMillis = null;
		Long endTimeMillis = null;
		try {
			String biast = "";
			if (deviceConfiguration.isBiast()) {
				biast = " --bias ";
			}
			startTimeMillis = System.currentTimeMillis();
			String commandLine = config.getProperty("satellites.satdump.path") + " live " + transmitter.getSatdumpPipeline() + " " + outputDirectory.getAbsolutePath() + " --source " + deviceConfiguration.getDeviceType().getSatdumpCode() + " --gain " + deviceConfiguration.getGain() + biast + " --samplerate "
					+ transmitter.getBandwidth() + " --frequency " + transmitter.getFrequency() + " --general_gain " + deviceConfiguration.getGain() + " --timeout " + ((req.getEndTimeMillis() - req.getStartTimeMillis()) / 1000) + " --tle_override explicitly_missing ";
			if (deviceConfiguration.getDeviceType().equals(DeviceType.SPYSERVER)) {
				commandLine += " --ip_address " + deviceConfiguration.getHost() + " --port " + deviceConfiguration.getPort();
			}
			satdump = factory.create(commandLine, Redirect.INHERIT, true);
			int responseCode = satdump.waitFor();
			if (responseCode != 0) {
				LOG.error("[{}] invalid response code from satdump: {}", req.getId(), responseCode);
				Util.deleteDirectory(outputDirectory.toPath());
			} else {
				LOG.info("[{}] satdump stopped: {}", req.getId(), responseCode);
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to run", req.getId(), e);
		} finally {
			Util.shutdown("satdump", satdump, 10000);
			endTimeMillis = System.currentTimeMillis();
		}
		LOG.info("[{}] observation completed", req.getId());
		IQData result = new IQData();
		if (outputDirectory.exists()) {
			result.setDataFile(findFile(outputDirectory));
		}
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFormat(DataFormat.UNKNOWN);
		return result;
	}

	private static File findFile(File basedir) {
		File[] files = basedir.listFiles();
		for (File cur : files) {
			if (cur.getName().endsWith(".raw16")) {
				return cur;
			}
			if (cur.getName().endsWith(".cadu")) {
				return cur;
			}
		}
		return null;
	}

	@Override
	public void complete() {
		Util.shutdown("satdump", satdump, 10000);
		satdump = null;
	}

}
