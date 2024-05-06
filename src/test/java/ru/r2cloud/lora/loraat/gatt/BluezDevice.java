package ru.r2cloud.lora.loraat.gatt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bluez.Device1;
import org.bluez.exceptions.BluezAlreadyConnectedException;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezAuthenticationCanceledException;
import org.bluez.exceptions.BluezAuthenticationFailedException;
import org.bluez.exceptions.BluezAuthenticationRejectedException;
import org.bluez.exceptions.BluezAuthenticationTimeoutException;
import org.bluez.exceptions.BluezConnectionAttemptFailedException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAvailableException;
import org.bluez.exceptions.BluezNotConnectedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

public class BluezDevice implements Properties, Device1 {

	private final String objectPath;
	private final List<BleService> services;
	private final String address;

	public BluezDevice(String objectPath, String address, BleService... services) {
		this.objectPath = objectPath;
		this.services = new ArrayList<>();
		for (BleService cur : services) {
			this.services.add(cur);
		}
		this.address = address;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	public List<BleService> getServices() {
		return services;
	}

	@Override
	public Map<String, Variant<?>> GetAll(String _interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		result.put("Address", new Variant<>(address));
		result.put("Connected", new Variant<>(Boolean.TRUE));
		return result;
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
	public void Connect() throws BluezNotReadyException, BluezFailedException, BluezInProgressException, BluezAlreadyConnectedException {
		// do nothing
	}

	@Override
	public void Disconnect() throws BluezNotConnectedException {
		// do nothing
	}

	@Override
	public void ConnectProfile(String _uuid) throws BluezFailedException, BluezInProgressException, BluezInvalidArgumentsException, BluezNotAvailableException, BluezNotReadyException {
		// do nothing
	}

	@Override
	public void DisconnectProfile(String _uuid) throws BluezFailedException, BluezInProgressException, BluezInvalidArgumentsException, BluezNotSupportedException {
		// do nothing
	}

	@Override
	public void Pair()
			throws BluezInvalidArgumentsException, BluezFailedException, BluezAlreadyExistsException, BluezAuthenticationCanceledException, BluezAuthenticationFailedException, BluezAuthenticationRejectedException, BluezAuthenticationTimeoutException, BluezConnectionAttemptFailedException {
		// do nothing
	}

	@Override
	public void CancelPairing() throws BluezDoesNotExistException, BluezFailedException {
		// do nothing
	}

}
