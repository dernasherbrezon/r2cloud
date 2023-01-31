package ru.r2cloud.lora.loraat.gatt;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;

public class BluezApplication implements ObjectManager {

	private final BluezManager manager;

	public BluezApplication(BluezManager manager) {
		this.manager = manager;
	}

	@Override
	public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects() {
		Map<DBusPath, Map<String, Map<String, Variant<?>>>> result = new HashMap<>();
		Map<String, Map<String, Variant<?>>> value = new HashMap<>();
		value.put("org.bluez.GattManager1", manager.GetAll(null));
		result.put(new DBusPath(manager.getObjectPath()), value);
		return result;
	}

	@Override
	public String getObjectPath() {
		return "/";
	}

}