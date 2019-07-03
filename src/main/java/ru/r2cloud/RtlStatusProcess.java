package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.RtlSdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

class RtlStatusProcess {

	private static final Logger LOG = LoggerFactory.getLogger(RtlStatusProcess.class);
	private static final Pattern DEVICEPATTERN = Pattern.compile("^  0:  (.*?), (.*?), SN: (.*?)$");

	private Process process;
	private boolean terminated = false;
	private final Configuration config;

	RtlStatusProcess(Configuration config) {
		this.config = config;
	}

	RtlSdrStatus getStatus() {
		RtlSdrStatus result = null;
		try {
			BufferedReader r = null;
			synchronized (this) {
				if (terminated) {
					terminated = false;
					return result;
				}
				process = new ProcessBuilder().command(config.getProperty("rtltest.path"), "-t").start();
				r = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				terminated = false;
			}
			String curLine = null;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				if (curLine.startsWith("No supported")) {
					result = new RtlSdrStatus();
					result.setDongleConnected(false);
					break;
				} else {
					Matcher m = DEVICEPATTERN.matcher(curLine);
					if (m.find()) {
						result = new RtlSdrStatus();
						result.setDongleConnected(true);
						result.setVendor(m.group(1));
						result.setChip(m.group(2));
						result.setSerialNumber(m.group(3));
						break;
					}
				}
			}
		} catch (IOException e) {
			String error = "unable to read status";
			result = new RtlSdrStatus();
			result.setError(error);
			LOG.error(error, e);
		} finally {
			stop(5000);
		}
		return result;
	}

	synchronized void terminate(long timeout) {
		shutdown(timeout);
		terminated = true;
	}

	synchronized void stop(long timeout) {
		shutdown(timeout);
		terminated = false;
	}

	private void shutdown(long timeout) {
		if (process == null) {
			return;
		}
		Util.shutdown("rtl-status", process, timeout);
		process = null;
	}
	
}
