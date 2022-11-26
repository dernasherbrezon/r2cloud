package ru.r2cloud.lora.loraat.gatt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.util.Configuration;

public class DeviceStatus extends BleCharacteristic {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceStatus.class);
	private final DeviceManager manager;

	public DeviceStatus(String objectPath, String[] flags, String uuId, String servicePath, DeviceManager manager) {
		super(objectPath, flags, uuId, servicePath);
		this.manager = manager;
	}

	@Override
	public byte[] read(String bluetoothAddress) {
		// unsupported
		return new byte[0];
	}

	@Override
	public void write(byte[] value, String bluetoothAddress) {
		if (value.length < 2) {
			LOG.info("not enough byte. expected 2, got: {}", value.length);
			return;
		}
		LoraAtBleDevice device = getLoraDevice(bluetoothAddress);
		if (device == null) {
			return;
		}
		device.setStatus(value[0] & 0xFF, (int) value[1]);
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
