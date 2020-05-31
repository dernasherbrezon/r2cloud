package ru.r2cloud.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SafeRunnable implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(SafeRunnable.class);

	@Override
	public void run() {
		try {
			safeRun();
		} catch (Exception e) {
			if (hasInterruptedCause(e)) {
				LOG.info("unable to continue: " + e.getMessage());
			} else {
				LOG.error("unable to run", e);
			}
		}
	}

	private static boolean hasInterruptedCause(Throwable e) {
		if (e instanceof InterruptedException) {
			Thread.currentThread().interrupt();
			return true;
		}
		if (e.getCause() != null) {
			return hasInterruptedCause(e.getCause());
		}
		return false;
	}

	public abstract void safeRun();

}
