package ru.r2cloud;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.Util;

public class ApplicationChecker {

	private final static Logger LOG = LoggerFactory.getLogger(ApplicationChecker.class);

	private final ProcessFactory factory;

	public ApplicationChecker(ProcessFactory factory) {
		this.factory = factory;
	}

	public boolean checkApplication(String name, String commandLine) {
		ProcessWrapper process = null;
		try {
			process = factory.create(commandLine, true, false);
			int code = process.waitFor();
			if (code == 0) {
				LOG.info("{} is installed", name);
				return true;
			}
			LOG.info("{} is not available. response code is: {}", name, code);
			return false;
		} catch (IOException e) {
			Util.logIOException(LOG, false, name + " is not available", e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
