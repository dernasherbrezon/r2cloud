package ru.r2cloud.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SafeRunnable implements Runnable {

	private final static Logger LOG = LoggerFactory.getLogger(SafeRunnable.class);

	@Override
	public void run() {
		try {
			doRun();
		} catch (Exception e) {
			LOG.error("uncaught exception", e);
		}
	}

	public abstract void doRun();
}
