package ru.r2cloud.device;

import ru.r2cloud.lora.loraat.gatt.GattClient;
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
import ru.r2cloud.satellite.reader.LoraAtBlecReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactory;

public class LoraAtBlecDevice extends Device {

	private final GattClient gattClient;
	private final Configuration config;

	public LoraAtBlecDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule, Configuration config, GattClient gattClient) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.config = config;
		this.gattClient = gattClient;
		this.gattClient.addDevice(deviceConfiguration.getHost());
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter transmitter, DeviceConfiguration deviceConfiguration) {
		return new LoraAtBlecReader(config, deviceConfiguration, req, gattClient, transmitter);
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setStatus(gattClient.getStatus(getDeviceConfiguration().getHost()));
		return result;
	}

}
