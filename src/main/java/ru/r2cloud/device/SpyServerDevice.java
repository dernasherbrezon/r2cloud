package ru.r2cloud.device;

import java.io.IOException;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
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

	private final Configuration config;

	public SpyServerDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			Configuration config, PredictOreKit predict, Schedule schedule) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter satellite, DeviceConfiguration deviceConfiguration) {
		return new SpyServerReader(config, req, deviceConfiguration, satellite);
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
			client.stop();
		} catch (IOException e) {
			status = new SpyServerStatus();
			status.setStatus(DeviceConnectionStatus.FAILED);
			status.setFailureMessage(e.getMessage());
		}
		result.setStatus(status.getStatus());
		if (status.getStatus().equals(DeviceConnectionStatus.FAILED)) {
			result.setFailureMessage(status.getFailureMessage());
		}
		return result;
	}

}
