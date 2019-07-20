package ru.r2cloud.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);
	private static final Pattern COMMA = Pattern.compile(",");

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

	public static boolean initDirectory(Path path) {
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
				return true;
			} catch (IOException e) {
				LOG.info("unable to create parent dir: " + path.toAbsolutePath(), e);
				return false;
			}
		}
		return true;
	}

	public static List<Path> toList(DirectoryStream<Path> stream) {
		List<Path> result = new ArrayList<>();
		for (Path cur : stream) {
			result.add(cur);
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
			LOG.error("executor did not terminate in the specified time: {}", threadpoolName);
		}
	}

	public static void shutdown(String name, ProcessWrapper process, long timeoutMillis) {
		if (process == null || !process.isAlive()) {
			return;
		}
		try {
			LOG.info("stopping: {}", name);
			process.destroy();
			if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
				LOG.info("unable to cleanly shutdown. kill process: {}", name);
				int statusCode = process.destroyForcibly().waitFor();
				if (statusCode != 0 && statusCode != 137) {
					LOG.info("invalid status code while stopping: {}", statusCode);
				}
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// TODO migrate to ProcessWrapper
	public static void shutdown(String name, Process process, long timeoutMillis) {
		if (process == null || !process.isAlive()) {
			return;
		}
		try {
			process.destroy();
			if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
				LOG.info("unable to cleanly shutdown. kill process: {}", name);
				int statusCode = process.destroyForcibly().waitFor();
				if (statusCode != 0 && statusCode != 137) {
					LOG.info("invalid status code while stopping: {}", statusCode);
				}
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static boolean deleteDirectory(Path f) {
		if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(f)) {
				for (Path entry : entries) {
					boolean curResult = deleteDirectory(entry);
					if (!curResult) {
						return curResult;
					}
				}
			} catch (IOException e) {
				LOG.error("unable to delete: " + f.toAbsolutePath(), e);
				return false;
			}
		}
		try {
			Files.delete(f);
			return true;
		} catch (IOException e) {
			LOG.error("unable to delete: " + f.toAbsolutePath(), e);
			return false;
		}
	}

	public static List<String> splitComma(String str) {
		String[] values = COMMA.split(str);
		List<String> result = new ArrayList<>();
		for (String cur : values) {
			cur = cur.trim();
			if (cur.length() == 0) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024 * 4];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	public static void closeQuietly(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			LOG.info("unable to close", e);
		}
	}

	// works well for files less than 4Gb
	public static Long readTotalSamples(File rawFile) {
		long totalSamples;
		try (RandomAccessFile raf = new RandomAccessFile(rawFile, "r")) {
			raf.seek(raf.length() - 4);
			long b4 = raf.read();
			long b3 = raf.read();
			long b2 = raf.read();
			long b1 = raf.read();
			totalSamples = ((b1 << 24) | (b2 << 16) + (b3 << 8) + b4) / 2;
		} catch (IOException e) {
			LOG.error("unable to get total number of samples", e);
			if (!rawFile.delete()) {
				LOG.error("unable to delete raw file at: {}", rawFile.getAbsolutePath());
			}
			return null;
		}
		return totalSamples;
	}

	private Util() {
		// do nothing
	}

}
