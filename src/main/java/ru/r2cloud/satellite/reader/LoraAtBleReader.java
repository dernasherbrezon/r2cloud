package ru.r2cloud.satellite.reader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;

public class LoraAtBleReader implements IQReader {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtBleReader.class);

	private final LoraAtBleDevice device;
	private final DeviceConfiguration deviceConfiguration;
	private final Configuration config;
	private final ObservationRequest req;
	private final Transmitter transmitter;
	private final CountDownLatch latch = new CountDownLatch(1);

	public LoraAtBleReader(Configuration config, DeviceConfiguration deviceConfiguration, ObservationRequest req, LoraAtBleDevice device, Transmitter transmitter) {
		this.device = device;
		this.deviceConfiguration = deviceConfiguration;
		this.config = config;
		this.req = req;
		this.transmitter = transmitter;
	}

	@Override
	public IQData start() throws InterruptedException {
		LOG.info("[{}] waiting for frames from lora-at: {}", req.getId(), toLoraAtFriendlyString());
		long startTimeMillis = System.currentTimeMillis();
		latch.await();
		if (device.getFrames().isEmpty()) {
			LOG.info("[{}] no frames received", req.getId());
			return null;
		}
		long endTimeMillis = System.currentTimeMillis();
		Path rawFile = config.getTempDirectoryPath().resolve(req.getSatelliteId() + "-" + req.getId() + ".raw");
		// raw file is the same as binary file
		// decoder just have to rename it
		try (BeaconOutputStream bos = new BeaconOutputStream(new BufferedOutputStream(Files.newOutputStream(rawFile)))) {
			for (LoraFrame cur : device.getFrames()) {
				bos.write(LoraAtReader.convert(cur));
			}
		} catch (IOException e) {
			LOG.error("[{}] unable to save beacons", req.getId(), e);
			return null;
		} finally {
			device.getFrames().clear();
		}
		LOG.info("[{}] observation completed", req.getId());
		IQData result = new IQData();
		result.setActualStart(startTimeMillis);
		result.setActualEnd(endTimeMillis);
		result.setIq(rawFile.toFile());
		result.setDataFormat(DataFormat.UNKNOWN);
		return result;
	}

	@Override
	public void complete() {
		latch.countDown();
	}

	private String toLoraAtFriendlyString() {
		return String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", transmitter.getFrequency(), transmitter.getLoraBandwidth(), transmitter.getLoraSpreadFactor(), transmitter.getLoraCodingRate(), transmitter.getLoraSyncword(), transmitter.getLoraPreambleLength(),
				(int) deviceConfiguration.getGain(), transmitter.getLoraLdro(), transmitter.isLoraCrc() ? 1 : 0, transmitter.isLoraExplicitHeader() ? 1 : 0, transmitter.getBeaconSizeBytes());
	}

}
