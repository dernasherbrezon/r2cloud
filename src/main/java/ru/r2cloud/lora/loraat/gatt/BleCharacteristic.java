package ru.r2cloud.lora.loraat.gatt;

import java.util.HashMap;
import java.util.Map;

import org.bluez.GattCharacteristic1;
import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidOffsetException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

public abstract class BleCharacteristic implements GattCharacteristic1, Properties {

	private final String objectPath;
	private final String[] flags;
	private final String uuId;
	private final String servicePath;

	public BleCharacteristic(String objectPath, String[] flags, String uuId, String servicePath) {
		this.objectPath = objectPath;
		this.flags = flags;
		this.uuId = uuId;
		this.servicePath = servicePath;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A Get(String interfaceName, String propertyName) {
		return (A) GetAll(null).get(propertyName).getValue();
	}

	@Override
	public <A> void Set(String interfaceName, String propertyName, A value) {
		// do nothing
	}

	@Override
	public Map<String, Variant<?>> GetAll(String interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		result.put("Service", new Variant<>(new DBusPath(servicePath)));
		result.put("UUID", new Variant<>(uuId));
		result.put("Flags", new Variant<>(flags));
		result.put("Descriptors", new Variant<>(new DBusPath[0]));
		return result;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	@Override
	public byte[] ReadValue(Map<String, Variant<?>> options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezInvalidOffsetException, BluezNotSupportedException {
		return read(convertBluetoothAddress(options));
	}

	@Override
	public void WriteValue(byte[] value, Map<String, Variant<?>> options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		write(value, convertBluetoothAddress(options));
	}

	public abstract byte[] read(String bluetoothAddress);

	public abstract void write(byte[] value, String bluetoothAddress);

	private static String convertBluetoothAddress(Map<String, Variant<?>> options) {
		Variant<?> device = options.get("device");
		if (device == null) {
			return null;
		}
		String deviceStr = device.getValue().toString();
		String prefix = "/dev_";
		int index = deviceStr.indexOf(prefix);
		if (index == -1) {
			return null;
		}
		return deviceStr.substring(index + prefix.length()).replace('_', ':');
	}

	@Override
	public TwoTuple<FileDescriptor, UInt16> AcquireWrite(Map<String, Variant<?>> options) throws BluezFailedException, BluezNotSupportedException {
		throw new BluezNotSupportedException("not supported");
	}

	@Override
	public TwoTuple<FileDescriptor, UInt16> AcquireNotify(Map<String, Variant<?>> options) throws BluezFailedException, BluezNotSupportedException {
		throw new BluezNotSupportedException("not supported");
	}

	@Override
	public void StartNotify() throws BluezFailedException, BluezNotPermittedException, BluezInProgressException, BluezNotSupportedException {
		throw new BluezNotSupportedException("not supported");
	}

	@Override
	public void StopNotify() throws BluezFailedException {
		throw new BluezFailedException("not supported");
	}

	@Override
	public void Confirm() throws BluezFailedException {
		throw new BluezFailedException("not supported");
	}

}
