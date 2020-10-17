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

public class PlutoSdrReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(PlutoSdrReader.class);

	private ProcessWrapper plutoSdrCli = null;

	private final Configuration config;
	private final ProcessFactory factory;
	private final ObservationRequest req;

	public PlutoSdrReader(Configuration config, ProcessFactory factory, ObservationRequest req) {
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
			startTimeMillis = System.currentTimeMillis();
			plutoSdrCli = factory.create(config.getProperty("satellites.plutosdr.wrapper.path") + " -cli " + config.getProperty("satellites.plutosdr.path") + " -f " + req.getActualFrequency() + " -s " + req.getInputSampleRate() + " -g " + req.getGain() + " -o " + rawFile.getAbsolutePath(), Redirect.INHERIT, false);
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
