package ru.r2cloud.device;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.loraat.gatt.LoraAtDeviceStatus;
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
import ru.r2cloud.satellite.reader.LoraAtBleReader;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ThreadPoolFactory;

public class LoraAtBleDevice extends Device {

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtBleDevice.class);

	private final Configuration config;
	private Integer batteryLevel;
	private Integer signalLevel;
	private List<LoraFrame> frames;

	public LoraAtBleDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
			PredictOreKit predict, Schedule schedule, Configuration config) {
		super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
		this.frames = new ArrayList<>();
		this.config = config;
	}

	@Override
	public IQReader createReader(ObservationRequest req, Transmitter transmitter, DeviceConfiguration deviceConfiguration) {
		return new LoraAtBleReader(config, deviceConfiguration, req, this, transmitter);
	}

	@Override
	public DeviceStatus getStatus() {
		DeviceStatus result = super.getStatus();
		if (signalLevel != null) {
			result.setStatus(DeviceConnectionStatus.CONNECTED);
		} else {
			result.setStatus(DeviceConnectionStatus.FAILED);
			result.setFailureMessage("Not connected yet");
		}
		result.setBatteryLevel(batteryLevel);
		result.setSignalLevel(signalLevel);
		return result;
	}

	public void updateStatus(LoraAtDeviceStatus status) {
		LOG.info("[{}] status: {}", id, status.toString());
		if (status.getBatteryVoltage() != 0) {
			double total = getDeviceConfiguration().getMaximumBatteryVoltage() - getDeviceConfiguration().getMinimumBatteryVoltage();
			// status battery voltage in mV
			this.batteryLevel = (int) (((status.getBatteryVoltage() / 1000.0 - getDeviceConfiguration().getMinimumBatteryVoltage()) / total) * 100);
		} else {
			this.batteryLevel = null;
		}
		this.signalLevel = status.getBluetoothRssi();
	}

	public void addFrame(LoraFrame frame) {
		frames.add(frame);
	}

	public List<LoraFrame> getFrames() {
		return frames;
	}

}
