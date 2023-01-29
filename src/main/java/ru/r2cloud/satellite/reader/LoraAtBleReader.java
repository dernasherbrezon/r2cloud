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
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;

public class LoraAtBleReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtBleReader.class);

	private final LoraAtBleDevice device;
	private final Configuration config;
	private final ObservationRequest req;
	private final Transmitter transmitter;
	private final CountDownLatch latch = new CountDownLatch(1);

	public LoraAtBleReader(Configuration config, ObservationRequest req, LoraAtBleDevice device, Transmitter transmitter) {
		this.device = device;
		this.config = config;
		this.req = req;
		this.transmitter = transmitter;
	}

	@Override
	public IQData start() throws InterruptedException {
		LOG.info("[{}] waiting for frames from lora-at: {}", req.getId(), toLoraAtFriendlyString(req, transmitter));
		long startTimeMillis = System.currentTimeMillis();
		latch.await();
		if (device.getFrames().isEmpty()) {
			LOG.info("[{}] no frames received", req.getId());
			return null;
		}
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

	private static String toLoraAtFriendlyString(ObservationRequest req, Transmitter transmitter) {
		return String.format("%f,%f,%d,%d,%d,%d,%d,%d,%d,%d,%d", transmitter.getFrequency() / 1000000.0f, transmitter.getLoraBandwidth() / 1000.0f, transmitter.getLoraSpreadFactor(), transmitter.getLoraCodingRate(), transmitter.getLoraSyncword(), 10, transmitter.getLoraPreambleLength(),
				(int) req.getGain(), transmitter.getLoraLdro(), transmitter.isLoraCrc() ? 1 : 0, transmitter.isLoraExplicitHeader() ? 1 : 0);
	}

}
