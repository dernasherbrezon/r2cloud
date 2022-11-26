package ru.r2cloud.device;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactory;

public class LoraAtBleDevice extends Device {

	private Integer batteryLevel;
	private Integer signalLevel;

	public LoraAtBleDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter satellite) {
		return null;
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		result.setType(DeviceType.LORA);
		if (signalLevel != null) {
			result.setStatus(DeviceConnectionStatus.CONNECTED);
		} else {
			result.setStatus(DeviceConnectionStatus.FAILED);
		}
		result.setBatteryLevel(batteryLevel);
		result.setSignalLevel(signalLevel);
		result.setFailureMessage("Not connected yet");
		return result;
	}

	public void setStatus(int batteryLevel, int signalLevel) {
		this.batteryLevel = batteryLevel;
		this.signalLevel = signalLevel;
	}

}
