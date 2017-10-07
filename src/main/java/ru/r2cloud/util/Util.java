package ru.r2cloud.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	public static File initDirectory(String path) {
		File result = new File(path);
		if (result.exists() && !result.isDirectory()) {
			throw new IllegalArgumentException("base path exists and not directory: " + result.getAbsolutePath());
		}
		if (!result.exists() && !result.mkdirs()) {
			throw new IllegalArgumentException("unable to create basepath: " + result.getAbsolutePath());
		}
		return result;
	}

	public static void toLog(Logger log, InputStream is) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
			String curLine = null;
			while ((curLine = in.readLine()) != null) {
				log.info(curLine);
			}
		}
	}

	public static void shutdown(ScheduledExecutorService executor, long timeoutMillis) {
		if (executor == null) {
			return;
		}
		executor.shutdownNow();
		boolean cleanlyTerminated;
		try {
			cleanlyTerminated = executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			cleanlyTerminated = executor.isTerminated();
		}
		if (!cleanlyTerminated) {
			String threadpoolName;
			if (executor instanceof ScheduledThreadPoolExecutor) {
				ThreadFactory factory = ((ScheduledThreadPoolExecutor) executor).getThreadFactory();
				if (factory instanceof NamingThreadFactory) {
					NamingThreadFactory namingFactory = (NamingThreadFactory) factory;
					threadpoolName = namingFactory.getPrefix();
				} else {
					threadpoolName = "unknown[" + factory.getClass().getSimpleName() + "]";
				}
			} else {
				threadpoolName = "unknown[" + executor.getClass().getSimpleName() + "]";
			}
			LOG.error("executor did not terminate in the specified time: " + threadpoolName);
		}
	}

	public static void shutdown(String name, Process process, long timeoutMillis) {
		if (process == null || !process.isAlive()) {
			return;
		}
		try {
			process.destroy();
			if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
				LOG.info("unable to cleanly shutdown. kill process: " + name);
				int statusCode = process.destroyForcibly().waitFor();
				if (statusCode != 0) {
					LOG.info("invalid status code while stopping: " + statusCode);
				}
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void deleteDirectory(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				deleteDirectory(c);
			}
		}
		if (!f.delete()) {
			LOG.error("Failed to delete file: " + f);
		}
	}

	private Util() {
		// do nothing
	}

}
