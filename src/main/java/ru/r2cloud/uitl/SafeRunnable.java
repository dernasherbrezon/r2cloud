package ru.r2cloud.uitl;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SafeRunnable implements Runnable {

	private final static Logger LOG = Logger.getLogger(SafeRunnable.class.getName());

	@Override
	public void run() {
		try {
			doRun();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "uncaught exception", e);
		}
	}

	public abstract void doRun();
}
