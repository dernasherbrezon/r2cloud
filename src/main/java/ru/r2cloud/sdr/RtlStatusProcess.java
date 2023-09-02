package ru.r2cloud.sdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class RtlStatusProcess implements SdrStatusProcess {

	private static final Logger LOG = LoggerFactory.getLogger(RtlStatusProcess.class);
	private static final Pattern DEVICEPATTERN = Pattern.compile("^  (\\d+):  (.*), (.*), SN: (.*)$");

	private final Configuration config;
	private final ProcessFactory factory;
	private final int expectedRtlDeviceId;

	public RtlStatusProcess(Configuration config, ProcessFactory factory, int expectedRtlDeviceId) {
		this.config = config;
		this.factory = factory;
		this.expectedRtlDeviceId = expectedRtlDeviceId;
	}

	@Override
	public SdrStatus getStatus() {
		SdrStatus result = null;
		try {
			BufferedReader r = null;
			ProcessWrapper process = factory.create(config.getProperty("satellites.rtlsdr.test.path") + " -t", false, false);
			r = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String curLine = null;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.startsWith("No supported")) {
					result = new SdrStatus();
					result.setStatus(DeviceConnectionStatus.FAILED);
					result.setFailureMessage(curLine);
					break;
				} else {
					Matcher m = DEVICEPATTERN.matcher(curLine);
					if (m.find()) {
						Integer actualDeviceId = parse(m.group(1));
						if (actualDeviceId == null || actualDeviceId != expectedRtlDeviceId) {
							continue;
						}
						result = new SdrStatus();
						result.setStatus(DeviceConnectionStatus.CONNECTED);
						result.setModel(m.group(2) + ", " + m.group(3) + ", SN: " + m.group(4));
						break;
					}
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
	
	private static Integer parse(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
