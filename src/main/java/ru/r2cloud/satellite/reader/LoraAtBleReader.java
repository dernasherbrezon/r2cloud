package ru.r2cloud.satellite.reader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.util.Configuration;

public class LoraAtBleReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtBleReader.class);

	private final LoraAtBleDevice device;
	private final Configuration config;
	private final ObservationRequest req;
	private final CountDownLatch latch = new CountDownLatch(1);

	public LoraAtBleReader(Configuration config, ObservationRequest req, LoraAtBleDevice device) {
		this.device = device;
		this.config = config;
		this.req = req;
	}

	@Override
	public IQData start() throws InterruptedException {
		LOG.info("waiting for messages from lora-at");
		long startTimeMillis = System.currentTimeMillis();
		latch.await();
		long endTimeMillis = System.currentTimeMillis();
		File rawFile = new File(config.getTempDirectory(), req.getSatelliteId() + "-" + req.getId() + ".raw");
		// raw file is the same as binary file
		// decoder just have to rename it
		try (BeaconOutputStream bos = new BeaconOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)))) {
			for (LoraFrame cur : device.getFrames()) {
				bos.write(LoraAtReader.convert(cur));
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to save beacons", req.getId(), e);
		}
		device.getFrames().clear();
		LOG.info("[{}] observation completed", req.getId());
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setDataFile(rawFile);
		return result;
	}

	@Override
	public void complete() {
		latch.countDown();
	}

}
