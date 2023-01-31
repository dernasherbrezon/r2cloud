package ru.r2cloud.lora.loraat.gatt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bluez.GattManager1;
import org.bluez.LEAdvertisingManager1;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezInvalidLengthException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

public class BluezManager implements GattManager1, LEAdvertisingManager1, Properties {

	private final List<ApplicationInfo> registeredApplications = new ArrayList<>();

	@Override
	public String getObjectPath() {
		return "/org/bluez/hci0";
	}

	@Override
	public Map<String, Variant<?>> GetAll(String _interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		return result;
	}

	@Override
	public <A> A Get(String _interfaceName, String _propertyName) {
		return null;
	}

	@Override
	public <A> void Set(String _interfaceName, String _propertyName, A _value) {
	}

	@Override
	public void RegisterAdvertisement(DBusPath _advertisement, Map<String, Variant<?>> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException, BluezInvalidLengthException, BluezNotPermittedException {
	}

	@Override
	public void UnregisterAdvertisement(DBusPath _advertisement) throws BluezInvalidArgumentsException, BluezDoesNotExistException {
	}

	@Override
	public void RegisterApplication(DBusPath _application, Map<String, Variant<?>> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException {
		ApplicationInfo app = new ApplicationInfo();
		app.setSource(AbstractConnection.getCallInfo().getSource());
		app.setObjectPath(_application.getPath());
		registeredApplications.add(app);
	}

	@Override
	public void UnregisterApplication(DBusPath _application) throws BluezInvalidArgumentsException, BluezDoesNotExistException {
	}

	public List<ApplicationInfo> getRegisteredApplications() {
		return registeredApplications;
	}

}
