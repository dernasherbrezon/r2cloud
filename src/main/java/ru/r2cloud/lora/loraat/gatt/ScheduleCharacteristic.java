package ru.r2cloud.lora.loraat.gatt;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.device.LoraAtDevice;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.util.Configuration;

public class ScheduleCharacteristic extends BleCharacteristic {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleCharacteristic.class);
	private final DeviceManager manager;

	public ScheduleCharacteristic(String objectPath, String[] flags, String uuId, String servicePath, DeviceManager manager) {
		super(objectPath, flags, uuId, servicePath);
		this.manager = manager;
	}

	@Override
	public byte[] read(String bluetoothAddress) {
		LoraAtBleDevice device = getLoraDevice(bluetoothAddress);
		if (device == null) {
			return new byte[0];
		}
		List<ObservationRequest> requests = device.findScheduledObservations();
		if (requests.isEmpty()) {
			return new byte[0];
		}
		Collections.sort(requests, ObservationRequestComparator.INSTANCE);

		ObservationRequest req = requests.get(0);
		// FIXME
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(byte[] value, String bluetoothAddress) {
		// TODO Auto-generated method stub

	}

	private LoraAtBleDevice getLoraDevice(String bluetoothAddress) {
		Device device = manager.findDeviceById(Configuration.LORA_AT_DEVICE_PREFIX + bluetoothAddress);
		if (device == null) {
			LOG.info("unknown bluetooth device: {}", bluetoothAddress);
			return null;
		}
		if (!(device instanceof LoraAtBleDevice)) {
			LOG.info("not a lora-at device: {}", bluetoothAddress);
			return null;
		}
		return (LoraAtBleDevice) device;
	}

}
