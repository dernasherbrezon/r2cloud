package ru.r2cloud.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SatdumpLogProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SatdumpLogProcessor.class);

	private final String id;
	private final InputStream is;

	public SatdumpLogProcessor(String id, InputStream is) {
		this.id = id;
		this.is = is;
	}

	public void start() {
		Thread tis = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					String curLine = null;
					while ((curLine = r.readLine()) != null) {
						if (curLine.endsWith("[m")) {
							curLine = curLine.substring(0, curLine.length() - 2);
						}
						String message = match(curLine, "(I)");
						if (message != null) {
							if (accept(message)) {
								LOG.info("[{}] {}", id, message);
							}
							continue;
						}
						message = match(curLine, "(E)");
						if (message != null) {
							if (accept(message)) {
								LOG.error("[{}] {}", id, message);
							}
							continue;
						}
						message = match(curLine, "(W)");
						if (message != null) {
							if (accept(message)) {
								LOG.warn("[{}] {}", id, message);
							}
							continue;
						}
						message = match(curLine, "(D)");
						if (message != null) {
							if (accept(message)) {
								LOG.debug("[{}] {}", id, message);
							}
							continue;
						}
						LOG.info(curLine);
					}
					r.close();
				} catch (Exception e) {
					LOG.error("[{}] unable to read input", id, e);
				}
			}
		}, "satdump-daemon");
		tis.setDaemon(true);
		tis.start();
	}

	private static String match(String curLine, String level) {
		int index = curLine.indexOf(level);
		if (index == -1) {
			return null;
		}
		return curLine.substring(index + 3).trim();
	}

	private static boolean accept(String str) {
		// remove any progress indicators
		if (str.contains("Wrote ")) {
			return false;
		}
		if (str.startsWith("Progress ")) {
			return false;
		}
		// remove ascii art
		if (str.contains("__") || str.contains("/_/")) {
			return false;
		}
		// remove debug
		if (str.startsWith("Loading ")) {
			return false;
		}
		return true;
	}

}
