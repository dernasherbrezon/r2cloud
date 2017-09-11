package ru.r2cloud.util;

import java.util.logging.LogManager;

public class ShutdownLoggingManager extends LogManager {
	static ShutdownLoggingManager instance;

	public ShutdownLoggingManager() {
		instance = this;
	}

	@Override
	public void reset() { /* don't reset yet. */
	}

	private void reset0() {
		super.reset();
	}

	public static void resetFinally() {
		instance.reset0();
	}
}