package ru.r2cloud.device;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.SpyServerReader;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyServerStatus;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactory;

public class SpyServerDevice extends Device {

	private static final Logger LOG = LoggerFactory.getLogger(SpyServerDevice.class);

	private final Configuration config;
	private final ReentrantLock lock = new ReentrantLock();
	private SpyServerStatus previousStatus;

	public SpyServerDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			Configuration config, PredictOreKit predict, Schedule schedule, SpyServerStatus initial) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
		this.previousStatus = initial;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter satellite, DeviceConfiguration deviceConfiguration) {
		return new SpyServerReader(config, req, deviceConfiguration, satellite, lock);
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setDeviceName("SpyServer - " + result.getConfig().getHost() + ":" + result.getConfig().getPort());
		SpyServerStatus spyServerStatus = getStatusInternal(result.getConfig());
		result.setFailureMessage(spyServerStatus.getFailureMessage());
		result.setStatus(spyServerStatus.getStatus());
		result.setModel(spyServerStatus.getDeviceSerial());
		return result;
	}

	private SpyServerStatus getStatusInternal(DeviceConfiguration deviceConfig) {
		try {
			if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
				try {
					SpyClient client = new SpyClient(deviceConfig.getHost(), deviceConfig.getPort(), deviceConfig.getTimeout());
					client.start();
					previousStatus = client.getStatus();
					client.stop();
				} finally {
					lock.unlock();
				}
			} else {
				LOG.info("can't get status within specified timeout. returning previous status");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return previousStatus;

	}

}
