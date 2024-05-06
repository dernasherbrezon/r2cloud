package ru.r2cloud.lora.loraat.gatt;

import java.util.HashMap;
import java.util.Map;

import org.bluez.Adapter1;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

public class BluezAdapter implements Adapter1, Properties {

	private final String objectPath;
	private final String bluetoothAddress;

	public BluezAdapter(String objectPath, String bluetoothAddress) {
		this.objectPath = objectPath;
		this.bluetoothAddress = bluetoothAddress;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A Get(String _interfaceName, String _propertyName) {
		Variant<?> variant = GetAll(_interfaceName).get(_propertyName);
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
	public Map<String, Variant<?>> GetAll(String _interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		result.put("Address", new Variant<>(bluetoothAddress));
		return result;
	}

	@Override
	public void StartDiscovery() throws BluezNotReadyException, BluezFailedException {
		// do nothing
	}

	@Override
	public void StopDiscovery() throws BluezNotReadyException, BluezFailedException, BluezNotAuthorizedException {
		// do nothing
	}

	@Override
	public void RemoveDevice(DBusPath _device) throws BluezInvalidArgumentsException, BluezFailedException {
		// do nothing
	}

	@Override
	public void SetDiscoveryFilter(Map<String, Variant<?>> _filter) throws BluezNotReadyException, BluezNotSupportedException, BluezFailedException {
		// do nothing
	}

	@Override
	public String[] GetDiscoveryFilters() {
		return new String[0];
	}

	@Override
	public DBusPath ConnectDevice(Map<String, Variant<?>> _properties) throws BluezInvalidArgumentsException, BluezAlreadyExistsException, BluezNotSupportedException, BluezNotReadyException, BluezFailedException {
		return null;
	}

}
