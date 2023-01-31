package ru.r2cloud.lora.loraat.gatt;

import java.util.HashMap;
import java.util.Map;

import org.bluez.LEAdvertisement1;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BleAdvertisement implements LEAdvertisement1, Properties {

	private static final Logger LOG = LoggerFactory.getLogger(BleAdvertisement.class);

	private final String objectPath;
	private final String localName;
	private final String type;
	private final String[] services;

	public BleAdvertisement(String objectPath, String localName, String type, String... services) {
		this.objectPath = objectPath;
		this.localName = localName;
		this.type = type;
		this.services = services;
	}

	@Override
	public String getObjectPath() {
		return objectPath;
	}

	@Override
	public void Release() {
		LOG.info("advertisement was released");
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
		result.put("Type", new Variant<>(type));
		result.put("ServiceUUIDs", new Variant<>(services));
		result.put("IncludeTxPower", new Variant<>(true));
		result.put("LocalName", new Variant<>(localName));
		return result;
	}

}
