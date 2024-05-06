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

public class BluezCharacteristic implements GattCharacteristic1, Properties {

	private final String objectPath;
	private final String uuid;
	private final String servicePath;

	private byte[] value;

	public BluezCharacteristic(String objectPath, String uuid, String servicePath) {
		this.objectPath = objectPath;
		this.uuid = uuid;
		this.servicePath = servicePath;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A Get(String interfaceName, String propertyName) {
		Variant<?> variant = GetAll(interfaceName).get(propertyName);
		if (variant == null) {
			return null;
		}
		return (A) variant.getValue();
	}

	@Override
	public <A> void Set(String _interfaceName, String _propertyName, A _value) {
		// do nothing
	}

	@Override
	public Map<String, Variant<?>> GetAll(String interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		result.put("Service", new Variant<>(new DBusPath(servicePath)));
		result.put("UUID", new Variant<>(uuid));
		return result;
	}

	@Override
	public byte[] ReadValue(Map<String, Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezInvalidOffsetException, BluezNotSupportedException {
		return value;
	}

	@Override
	public void WriteValue(byte[] _value, Map<String, Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		this.value = _value;
	}

	@Override
	public TwoTuple<FileDescriptor, UInt16> AcquireWrite(Map<String, Variant<?>> _options) throws BluezFailedException, BluezNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TwoTuple<FileDescriptor, UInt16> AcquireNotify(Map<String, Variant<?>> _options) throws BluezFailedException, BluezNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void StartNotify() throws BluezFailedException, BluezNotPermittedException, BluezInProgressException, BluezNotSupportedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void StopNotify() throws BluezFailedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void Confirm() throws BluezFailedException {
		// TODO Auto-generated method stub

	}

}
