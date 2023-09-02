package ru.r2cloud.satellite.reader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.spyclient.OnDataCallback;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SpyServerReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerReader.class);

	private final Configuration config;
	private final ObservationRequest req;

	private SpyClient client;
	private CountDownLatch latch = new CountDownLatch(1);
	private Long startTimeMillis = null;

	public SpyServerReader(Configuration config, ObservationRequest req) {
		this.config = config;
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		client = new SpyClient(req.getSpyServerConfiguration().getHost(), req.getSpyServerConfiguration().getPort(), req.getSpyServerConfiguration().getTimeout());
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + "." + client.getStatus().getFormat().getExtension());
		Long endTimeMillis = null;
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(rawFile))) {
			client.start();
			client.setGain((long) req.getGain());
			client.setFrequency(req.getActualFrequency());
			client.startStream(new OnDataCallback() {

				private byte[] buffer = new byte[4096];

				@Override
				public boolean onData(InputStream is, int len) {
					int remaining = len;
					if (SpyServerReader.this.startTimeMillis == null) {
						SpyServerReader.this.startTimeMillis = System.currentTimeMillis();
					}
					while (remaining > 0) {
						try {
							int toRead;
							if (buffer.length > remaining) {
								toRead = remaining;
							} else {
								toRead = buffer.length;
							}
							int actualRead = is.read(buffer, 0, toRead);
							if (actualRead < 0) {
								LOG.info("[{}] no more data", req.getId());
								break;
							}
							os.write(buffer, 0, actualRead);
							remaining -= actualRead;
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
					}
					return true;
				}
			});
			latch.await();
			endTimeMillis = System.currentTimeMillis();
		} catch (IOException e) {
			Util.logIOException(LOG, "[" + req.getId() + "] unable to start client", e);
			return null;
		}

		try {
			client.stopStream();
		} catch (IOException e) {
			Util.logIOException(LOG, "[" + req.getId() + "]unable to gracefully stop", e);
		}
		client.stop();

		IQData result = new IQData();
		if (startTimeMillis != null) {
			result.setActualStart(startTimeMillis);
		} else {
			result.setActualStart(endTimeMillis);
		}
		result.setActualEnd(endTimeMillis);
		result.setDataFile(rawFile);
		return result;
	}

	@Override
	public void complete() {
		latch.countDown();
	}

}