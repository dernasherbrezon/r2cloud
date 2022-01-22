package ru.r2cloud.sdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

class PlutoStatusProcess implements SdrStatusProcess {

	private static final Logger LOG = LoggerFactory.getLogger(PlutoStatusProcess.class);

	private ProcessWrapper process;
	private final Configuration config;
	private final ProcessFactory factory;

	PlutoStatusProcess(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public SdrStatus getStatus() {
		SdrStatus result = null;
		try {
			BufferedReader r = null;
			process = factory.create(config.getProperty("satellites.plutosdr.test.path") + " -a", false, false);
			r = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String curLine = null;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.startsWith("No IIO context")) {
					result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.FAILED);
					result.setFailureMessage(curLine);
					break;
				}
			}
			// TODO find correct plutosdr output
		} catch (IOException e) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage(e.getMessage());
			Util.logIOException(LOG, "unable to read status", e);
		}
		if (result == null) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage("unable to read device status");
		}
		return result;
	}

}
