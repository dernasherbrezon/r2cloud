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

public class PlutoStatusProcess implements SdrStatusProcess {

	private static final Logger LOG = LoggerFactory.getLogger(PlutoStatusProcess.class);
	private static final String HW_MODEL = "hw_model:";

	private final Configuration config;
	private final ProcessFactory factory;

	public PlutoStatusProcess(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public SdrStatus getStatus() {
		SdrStatus result = null;
		try {
			BufferedReader r = null;
			ProcessWrapper process = factory.create(config.getProperty("satellites.plutosdr.test.path") + " -a", false, false);
			r = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String curLine = null;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.startsWith("No IIO context")) {
					result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.FAILED);
					result.setFailureMessage(curLine);
					break;
				}
				int index = curLine.indexOf(HW_MODEL);
				if (index != -1) {
					result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.CONNECTED);
					result.setModel(curLine.substring(HW_MODEL.length() + 1).trim());
					break;
				}
			}
		} catch (IOException e) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage(e.getMessage());
			Util.logIOException(LOG, "unable to read status", e);
		}
		if (result == null) {
			result = new SdrStatus();
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage("unable to find device");
		}
		return result;
	}

}
