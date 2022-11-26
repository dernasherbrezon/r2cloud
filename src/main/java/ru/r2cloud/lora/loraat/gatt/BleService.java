package ru.r2cloud.lora.loraat.gatt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bluez.GattService1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

public class BleService implements GattService1, Properties {

	private final String objectPath;
	private final String uuid;
	private final boolean primary;
	private final DBusPath[] paths;
	private final List<BleCharacteristic> characteristics;

	public BleService(String objectPath, String uuid, boolean primary, List<BleCharacteristic> characteristics) {
		this.objectPath = objectPath;
		this.uuid = uuid;
		this.primary = primary;
		this.paths = new DBusPath[characteristics.size()];
		for (int i = 0; i < characteristics.size(); i++) {
			this.paths[i] = new DBusPath(characteristics.get(i).getObjectPath());
		}
		this.characteristics = characteristics;
	}

	@Override
	public Map<String, Variant<?>> GetAll(String interfaceName) {
		Map<String, Variant<?>> result = new HashMap<>();
		result.put("UUID", new Variant<>(uuid));
		result.put("Primary", new Variant<>(primary));
		result.put("Characteristics", new Variant<>(paths));
		return result;
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
	public String getObjectPath() {
		return objectPath;
	}

	public List<BleCharacteristic> getCharacteristics() {
		return characteristics;
	}

}
