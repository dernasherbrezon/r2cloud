package ru.r2cloud.satellite;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class FramingFilter implements TransmitterFilter {

	private static final Logger LOG = LoggerFactory.getLogger(FramingFilter.class);

	private final Configuration config;
	private final ProcessFactory factory;
	private Boolean satdumpAvailable = null;
	private Boolean wxtoimgAvailable = null;

	public FramingFilter(Configuration config, ProcessFactory factory) {
		this.config = config;
		this.factory = factory;
	}

	@Override
	public boolean accept(Transmitter transmitter) {
		if (transmitter.getFraming() == null) {
			return true;
		}
		if (!config.getBoolean("satellits.validate.external")) {
			return true;
		}
		if (transmitter.getFraming().equals(Framing.SATDUMP)) {
			if (satdumpAvailable == null) {
				satdumpAvailable = checkSatdump();
			}
			return satdumpAvailable;
		}
		if (transmitter.getFraming().equals(Framing.APT)) {
			if (wxtoimgAvailable == null) {
				wxtoimgAvailable = checkWxtoimg();
			}
			return wxtoimgAvailable;
		}
		return true;
	}

	private Boolean checkWxtoimg() {
		ProcessWrapper process = null;
		String commandLine = config.getProperty("satellites.wxtoimg.path") + " -h";
		try {
			process = factory.create(commandLine, true, false);
			int code = process.waitFor();
			if (code == 0) {
				LOG.info("wxtoimg is installed");
				return true;
			}
			LOG.info("wxtoimg is not available. response code is: {}", code);
			return false;
		} catch (IOException e) {
			Util.logIOException(LOG, false, "wxtoimg is not available", e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private boolean checkSatdump() {
		ProcessWrapper process = null;
		String commandLine = config.getProperty("satellites.satdump.path") + " version";
		try {
			process = factory.create(commandLine, true, false);
			int code = process.waitFor();
			if (code == 0) {
				LOG.info("satdump is installed");
				return true;
			}
			LOG.info("satdump is not available. response code is: {}", code);
			return false;
		} catch (IOException e) {
			Util.logIOException(LOG, false, "satdump is not available", e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
