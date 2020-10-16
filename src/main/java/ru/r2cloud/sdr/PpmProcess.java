package ru.r2cloud.sdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

class PpmProcess {

	private static final Logger LOG = LoggerFactory.getLogger(PpmProcess.class);
	private static final Pattern PPMPATTERN = Pattern.compile("real sample rate: \\d+ current PPM: \\d+ cumulative PPM: (\\d+)");

	private ProcessWrapper process;
	private boolean terminated = false;
	private final Configuration config;
	private final ProcessFactory factory;

	PpmProcess(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	Integer getPpm() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("ppm calculation");
		}
		Integer result = null;
		try {
			BufferedReader r = null;
			synchronized (this) {
				if (terminated) {
					terminated = false;
					return result;
				}
				process = factory.create(config.getProperty("stdbuf.path") + " -i 0 -o 0 -e 0 " + config.getProperty("rtltest.path")+ " -p2", true, false);
				r = new BufferedReader(new InputStreamReader(process.getInputStream()));
				terminated = false;
			}
			String curLine = null;
			int numberOfSamples = 0;
			while ((curLine = r.readLine()) != null && !Thread.currentThread().isInterrupted()) {
				LOG.info(curLine);
				if (curLine.startsWith("No supported")) {
					break;
				} else if (curLine.startsWith("real sample rate")) {
					numberOfSamples++;
					if (numberOfSamples >= 10) {
						Matcher m = PPMPATTERN.matcher(curLine);
						if (m.find()) {
							String ppmStr = m.group(1);
							result = Integer.valueOf(ppmStr);
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			LOG.error("unable to calculate ppm", e);
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
		Util.shutdown("ppm-test", process, timeout);
		process = null;
	}

}
