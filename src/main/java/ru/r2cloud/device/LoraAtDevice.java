package ru.r2cloud.device;

import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.loraat.LoraAtClient;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.LoraAtReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactory;

public class LoraAtDevice extends Device {

	private final LoraAtClient client;
	private final Configuration config;

	public LoraAtDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, ObservationDao observationDao, DecoderService decoderService,
			Configuration config, PredictOreKit predict, LoraAtClient client) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict);
		this.client = client;
		this.config = config;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter satellite) {
		return new LoraAtReader(config, req, client, satellite);
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setType(DeviceType.LORA);
		LoraStatus loraStatus = client.getStatus();
		result.setStatus(loraStatus.getDeviceStatus());
		if (loraStatus.getDeviceStatus().equals(DeviceConnectionStatus.FAILED)) {
			result.setFailureMessage(loraStatus.getStatus());
		}
		return result;
	}

}