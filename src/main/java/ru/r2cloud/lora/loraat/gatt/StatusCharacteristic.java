package ru.r2cloud.lora.loraat.gatt;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class StatusCharacteristic extends BleCharacteristic {

	private static final Logger LOG = LoggerFactory.getLogger(StatusCharacteristic.class);
	private static final int PROTOCOL_VERSION = 2;
	private final DeviceManager manager;

	public StatusCharacteristic(String objectPath, String[] flags, String uuId, String servicePath, BleDescriptor descriptor, DeviceManager manager) {
		super(objectPath, flags, uuId, servicePath, descriptor);
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
			LOG.info("[{}] not enough bytes. expected 2, got: {}", bluetoothAddress, value.length);
			return;
		}
		LoraAtBleDevice device = getLoraDevice(bluetoothAddress);
		if (device == null) {
			return;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		try (DataInputStream dis = new DataInputStream(bais)) {
			int protocolVersion = dis.readUnsignedByte();
			if (protocolVersion != PROTOCOL_VERSION) {
				LOG.error("[{}] invalid protocol version {}, expected {}", bluetoothAddress, protocolVersion, PROTOCOL_VERSION);
				return;
			}
			LoraAtDeviceStatus status = new LoraAtDeviceStatus();
			status.setBluetoothRssi(dis.readByte());
			status.setSx127xRawTemperature(dis.readByte());
			status.setSolarVoltage(dis.readUnsignedShort());
			status.setSolarCurrent(dis.readShort());
			status.setBatteryVoltage(dis.readUnsignedShort());
			status.setBatteryCurrent(dis.readShort());
			device.updateStatus(status);
		} catch (IOException e) {
			Util.logIOException(LOG, false, "[" + bluetoothAddress + "] can't read input", e);
			return;
		}

	}

	private LoraAtBleDevice getLoraDevice(String bluetoothAddress) {
		Device device = manager.findDeviceById(Configuration.LORA_AT_DEVICE_PREFIX + bluetoothAddress);
		if (device == null) {
			LOG.info("[{}] ble device is not configured", bluetoothAddress);
			return null;
		}
		if (!(device instanceof LoraAtBleDevice)) {
			LOG.info("not a lora-at device: {}", bluetoothAddress);
			return null;
		}
		return (LoraAtBleDevice) device;
	}

}
