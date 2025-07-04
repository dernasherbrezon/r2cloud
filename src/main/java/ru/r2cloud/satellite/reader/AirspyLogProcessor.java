package ru.r2cloud.satellite.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirspyLogProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(AirspyLogProcessor.class);

	private final String id;
	private final InputStream is;
	private final String type;

	public AirspyLogProcessor(String id, InputStream is, String type) {
		this.id = id;
		this.is = is;
		this.type = type;
	}

	public void start() {
		Thread tis = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					String curLine = null;
					while ((curLine = r.readLine()) != null) {
						if (curLine.startsWith("Streaming at")) {
							continue;
						}
						LOG.info(curLine);
					}
					r.close();
				} catch (IOException e) {
					// do nothing. most likely process is down or stream was closed
				} catch (Exception e) {
					LOG.error("[{}] unable to read input", id, e);
				}
			}
		}, type);
		tis.setDaemon(true);
		tis.start();
	}

}
