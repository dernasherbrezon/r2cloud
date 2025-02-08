package ru.r2cloud.device;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.SatdumpReader;
import ru.r2cloud.satellite.reader.SpyServerReader;
import ru.r2cloud.spyclient.SpyClient;
import ru.r2cloud.spyclient.SpyServerStatus;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class SpyServerDevice extends Device {

	private final Configuration config;
	private final ProcessFactory processFactory;

	public SpyServerDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			Configuration config, PredictOreKit predict, Schedule schedule, ProcessFactory processFactory) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
		this.processFactory = processFactory;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter transmitter, DeviceConfiguration deviceConfiguration) {
		if (transmitter.getFraming() == Framing.SATDUMP) {
			return new SatdumpReader(config, deviceConfiguration, processFactory, req, transmitter, new ReentrantLock());
		} else {
			return new SpyServerReader(config, req, deviceConfiguration, transmitter);
		}
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setDeviceName("SpyServer - " + result.getConfig().getHost() + ":" + result.getConfig().getPort());
		SpyClient client = new SpyClient(result.getConfig().getHost(), result.getConfig().getPort(), result.getConfig().getTimeout());
		SpyServerStatus status;
		try {
			client.start();
			status = client.getStatus();
		} catch (IOException e) {
			status = new SpyServerStatus();
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage(e.getMessage());
		} finally {
			client.stop();
		}
		result.setStatus(status.getStatus());
		if (status.getStatus().equals(DeviceConnectionStatus.FAILED)) {
			result.setFailureMessage(status.getFailureMessage());
		}
		return result;
	}

}
