package ru.r2cloud.lora.loraat.gatt;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bluez.GattDescriptor1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

public class BleDescriptor implements Properties, GattDescriptor1 {
	
	private final String objectPath;
	private final String[] flags;
	private final String uuid;
	private final String characteristicPath;
	private final String value;

	public BleDescriptor(String objectPath, String[] flags, String uuid, String characteristicPath, String value) {
		this.objectPath = objectPath;
		this.flags = flags;
		this.uuid = uuid;
		this.characteristicPath = characteristicPath;
		this.value = value;
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
		result.put("Characteristic", new Variant<>(new DBusPath(characteristicPath)));
		result.put("UUID", new Variant<>(uuid));
		result.put("Flags", new Variant<>(flags));
		result.put("Value", new Variant<>(value));
		return result;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	@Override
	public byte[] ReadValue(Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException {
		return value.getBytes(StandardCharsets.ISO_8859_1);
	}

	@Override
	public void WriteValue(byte[] _value, Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		throw new BluezNotSupportedException("not supported");
	}

}
