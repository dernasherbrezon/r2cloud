package ru.r2cloud.satellite.reader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.spyclient.OnDataCallback;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyServerStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class SpyServerReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerReader.class);

	private final Configuration config;
	private final ObservationRequest req;
	private final DeviceConfiguration deviceConfiguraiton;
	private final Transmitter transmitter;
	private final ReentrantLock lock;

	private SpyClient client;
	private CountDownLatch latch = new CountDownLatch(1);
	private Long startTimeMillis = null;

	public SpyServerReader(Configuration config, ObservationRequest req, DeviceConfiguration deviceConfiguration, Transmitter transmitter, ReentrantLock lock) {
		this.config = config;
		this.req = req;
		this.deviceConfiguraiton = deviceConfiguration;
		this.transmitter = transmitter;
		this.lock = lock;
	}

	@Override
	public IQData start() throws InterruptedException {
		lock.lock();
		try {
			return startInternally();
		} finally {
			lock.unlock();
		}
	}

	private IQData startInternally() throws InterruptedException {
		client = new SpyClient(deviceConfiguraiton.getHost(), deviceConfiguraiton.getPort(), deviceConfiguraiton.getTimeout());
		client.start();
		SpyServerStatus status = client.getStatus();
		if (!status.getStatus().equals(DeviceConnectionStatus.CONNECTED)) {
			// stop anyway
			client.stop();
			return null;
		}
		Long sampleRate = getSampleRate(client, req.getId(), transmitter);
		if (sampleRate == null) {
			return null;
		}
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + "." + status.getFormat().getExtension());
		Long endTimeMillis = null;
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(rawFile))) {

			LOG.info("[{}] starting observation on {} with sample rate {} gain {}", req.getId(), req.getFrequency(), sampleRate, deviceConfiguraiton.getGain());

			client.setGain((long) deviceConfiguraiton.getGain());
			client.setFrequency(req.getFrequency());
			client.setSamplingRate(sampleRate);
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
							Util.logIOException(LOG, "unable to read data", e);
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
		} finally {
			try {
				client.stopStream();
			} catch (IOException e) {
				Util.logIOException(LOG, "[" + req.getId() + "] unable to gracefully stop", e);
			}
			client.stop();
		}

		IQData result = new IQData();
		if (startTimeMillis != null) {
			result.setActualStart(startTimeMillis);
		} else {
			result.setActualStart(endTimeMillis);
		}
		result.setActualEnd(endTimeMillis);
		result.setIq(rawFile);
		result.setDataFormat(status.getFormat());
		result.setSampleRate(sampleRate);
		return result;
	}

	@Override
	public void complete() {
		latch.countDown();
	}

	private static Long getSampleRate(SpyClient client, String id, Transmitter transmitter) {
		if (transmitter.getFraming() != null && transmitter.getFraming().equals(Framing.SATDUMP)) {
			// assume sorted asc
			for (Long cur : client.getStatus().getSupportedSampleRates()) {
				if (cur >= transmitter.getBandwidth()) {
					return cur;
				}
			}
			return null;
		}
		Integer maxBaudRate = Collections.max(transmitter.getBaudRates());
		if (maxBaudRate == null) {
			LOG.error("[{}] no configured baud raters", id);
			return null;
		}
		Long sampleRate = Util.getSmallestGoodDeviceSampleRate(maxBaudRate, client.getStatus().getSupportedSampleRates());
		if (sampleRate == null) {
			LOG.error("[{}] cannot find sample rate for: {}", id, maxBaudRate);
			return null;
		}
		return sampleRate;
	}

}
