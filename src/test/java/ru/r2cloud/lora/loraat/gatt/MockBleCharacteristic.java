package ru.r2cloud.lora.loraat.gatt;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;

public class MockBleCharacteristic extends BleCharacteristic {

	private byte[] value;
	private boolean notificationsEnabled;

	public MockBleCharacteristic(String objectPath, String[] flags, String uuid, String servicePath, BleDescriptor descriptor, byte[] value) {
		super(objectPath, flags, uuid, servicePath, descriptor);
		this.value = value;
	}

	@Override
	public byte[] read(String bluetoothAddress) {
		return value;
	}

	@Override
	public void write(byte[] value, String bluetoothAddress) {
		this.value = value;
	}

	@Override
	public void StartNotify() throws BluezFailedException, BluezNotPermittedException, BluezInProgressException, BluezNotSupportedException {
		notificationsEnabled = true;
	}

	@Override
	public void StopNotify() throws BluezFailedException {
		notificationsEnabled = false;
	}
	
	public boolean isNotificationsEnabled() {
		return notificationsEnabled;
	}

}
