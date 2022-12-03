package ru.r2cloud.lora.loraat.gatt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bluez.GattManager1;
import org.bluez.LEAdvertisingManager1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.device.DeviceManager;

public class GattServer implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(GattServer.class);

	private static String LORA_SERVICE_UUID = "3f5f0b4d-e311-4921-b29d-936afb8734cc";
	private static String SCHEDULE_CHARACTERISTIC_UUID = "40d6f70c-5e28-4da4-a99e-c5298d1613fe";
	private static String STATUS_CHARACTERISTIC_UUID = "5b53256e-76d2-4259-b3aa-15b5b4cfdd32";
	private static String LORA_SERVICE_PATH = "/org/bluez/r2cloud/service0";

	private final DeviceManager manager;
	private final BusAddress address;

	private DBusConnection dbusConn;
	private GattManager1 serviceManager;
	private LEAdvertisingManager1 advertisingManager;
	private BleApplication application;
	private BleAdvertisement advertisement;

	public GattServer(DeviceManager manager, BusAddress address) {
		this.manager = manager;
		this.address = address;
	}

	@Override
	public void start() {
		try {
			dbusConn = DBusConnectionBuilder.forAddress(address).withShared(false).build();
			LOG.info("dbus connected");
			ObjectManager adapter = dbusConn.getRemoteObject("org.bluez", "/", ObjectManager.class);
			if (adapter == null || adapter.GetManagedObjects() == null) {
				LOG.error("cannot find bluez");
				stop();
				return;
			}
			DBusPath serviceManagerPath = getServiceManagerPath(adapter);
			if (serviceManagerPath == null) {
				LOG.error("cannot find bluez service manager");
				stop();
				return;
			}
			serviceManager = dbusConn.getRemoteObject("org.bluez", serviceManagerPath.getPath(), GattManager1.class);
			advertisingManager = dbusConn.getRemoteObject("org.bluez", serviceManagerPath.getPath(), LEAdvertisingManager1.class);

			ScheduleCharacteristic schedule = new ScheduleCharacteristic(LORA_SERVICE_PATH + "/char0", new String[] { "read", "write" }, SCHEDULE_CHARACTERISTIC_UUID, LORA_SERVICE_PATH, manager);
			DeviceStatus status = new DeviceStatus(LORA_SERVICE_PATH + "/char1", new String[] { "write" }, STATUS_CHARACTERISTIC_UUID, LORA_SERVICE_PATH, manager);
			List<BleCharacteristic> characteristics = new ArrayList<>();
			characteristics.add(schedule);
			characteristics.add(status);
			BleService service = new BleService(LORA_SERVICE_PATH, LORA_SERVICE_UUID, true, characteristics);
			List<BleService> allServices = new ArrayList<>();
			allServices.add(service);
			application = new BleApplication(allServices);
			exportAll(dbusConn, application);

			serviceManager.RegisterApplication(new DBusPath(application.getObjectPath()), new HashMap<>());

			advertisement = new BleAdvertisement("/org/bluez/r2cloud/advertisement0", "r2cloud", "peripheral", LORA_SERVICE_UUID);
			dbusConn.exportObject(advertisement);
			LOG.info("Gatt application registered");

			advertisingManager.RegisterAdvertisement(new DBusPath(advertisement.getObjectPath()), new HashMap<>());
			LOG.info("Gatt application advertised");

		} catch (Exception e) {
			LOG.error("unable to start Gatt server", e);
			stop();
		}
	}

	@Override
	public void stop() {
		if (dbusConn == null) {
			return;
		}
		if (application != null) {
			try {
				serviceManager.UnregisterApplication(new DBusPath(application.getObjectPath()));
			} catch (Exception e) {
				LOG.error("cannot un-register application", e);
			}
			unExportAll(dbusConn, application);
			application = null;
			LOG.info("Gatt application stopped");
		}

		if (advertisement != null) {
			try {
				advertisingManager.UnregisterAdvertisement(new DBusPath(advertisement.getObjectPath()));
			} catch (Exception e) {
				LOG.error("cannot un-register advertisement", e);
			}
			dbusConn.unExportObject(advertisement.getObjectPath());
			advertisement = null;
			LOG.info("Gatt advertisement stopped");
		}

		dbusConn.disconnect();
		dbusConn = null;
		LOG.info("dbus disconnected");
	}

	private static void exportAll(DBusConnection dbusConn, BleApplication application) throws DBusException {
		dbusConn.requestBusName("com.github.loraat");
		for (BleService cur : application.getServices()) {
			for (BleCharacteristic curChar : cur.getCharacteristics()) {
				dbusConn.exportObject(curChar);
			}
			dbusConn.exportObject(cur);
		}
		dbusConn.exportObject(application);
	}

	private static void unExportAll(DBusConnection dbusConn, BleApplication application) {
		for (BleService cur : application.getServices()) {
			for (BleCharacteristic curChar : cur.getCharacteristics()) {
				dbusConn.unExportObject(curChar.getObjectPath());
			}
			dbusConn.unExportObject(cur.getObjectPath());
		}
		dbusConn.unExportObject(application.getObjectPath());
	}

	private static DBusPath getServiceManagerPath(ObjectManager adapter) {
		for (Entry<DBusPath, Map<String, Map<String, Variant<?>>>> cur : adapter.GetManagedObjects().entrySet()) {
			if (!cur.getValue().containsKey("org.bluez.GattManager1")) {
				continue;
			}
			return cur.getKey();
		}
		return null;
	}

}
