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
import ru.r2cloud.util.Clock;

public class GattServer implements Lifecycle {

	public static final String SCHEDULE_CHARACTERISTIC_UUID = "40d6f70c-5e28-4da4-a99e-c5298d1613fe";
	public static final String STATUS_CHARACTERISTIC_UUID = "5b53256e-76d2-4259-b3aa-15b5b4cfdd32";

	private static final Logger LOG = LoggerFactory.getLogger(GattServer.class);

	private static final String LORA_SERVICE_UUID = "3f5f0b4d-e311-4921-b29d-936afb8734cc";

	private static final String LORA_APPLICATION_PATH = "/org/bluez/r2cloud";
	private static final String LORA_ADVERTISEMENT_PATH = LORA_APPLICATION_PATH + "/advertisement0";
	private static final String LORA_SERVICE_PATH = LORA_APPLICATION_PATH + "/service0";
	private static final String LORA_SCHEDULE_PATH = LORA_SERVICE_PATH + "/char0";
	private static final String LORA_STATUS_PATH = LORA_SERVICE_PATH + "/char1";

	private final DeviceManager manager;
	private final BusAddress address;
	private final Clock clock;

	private DBusConnection dbusConn;
	private GattManager1 serviceManager;
	private LEAdvertisingManager1 advertisingManager;
	private BleApplication application;
	private BleAdvertisement advertisement;

	public GattServer(DeviceManager manager, BusAddress address, Clock clock) {
		this.manager = manager;
		this.address = address;
		this.clock = clock;
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

			BleDescriptor scheduleDesc = new BleDescriptor(LORA_SCHEDULE_PATH + "/desc0", new String[] { "read" }, "5604f205-0c14-4926-9d7d-21dbab315f2e", LORA_SCHEDULE_PATH, "Schedule for LoRa module");
			ScheduleCharacteristic schedule = new ScheduleCharacteristic(LORA_SCHEDULE_PATH, new String[] { "read", "write" }, SCHEDULE_CHARACTERISTIC_UUID, LORA_SERVICE_PATH, scheduleDesc, manager, clock);

			BleDescriptor statusDesc = new BleDescriptor(LORA_STATUS_PATH + "/desc0", new String[] { "read" }, "5604f205-0c14-4926-9d7d-21dbab315f2f", LORA_STATUS_PATH, "Status of all connected LoRa modules");
			DeviceStatus status = new DeviceStatus(LORA_STATUS_PATH, new String[] { "write" }, STATUS_CHARACTERISTIC_UUID, LORA_SERVICE_PATH, statusDesc, manager);
			List<BleCharacteristic> characteristics = new ArrayList<>();
			characteristics.add(schedule);
			characteristics.add(status);
			BleService service = new BleService(LORA_SERVICE_PATH, LORA_SERVICE_UUID, true, characteristics);
			List<BleService> allServices = new ArrayList<>();
			allServices.add(service);
			application = new BleApplication(LORA_APPLICATION_PATH, allServices);
			exportAll(dbusConn, application);

			serviceManager.RegisterApplication(new DBusPath(application.getObjectPath()), new HashMap<>());

			advertisement = new BleAdvertisement(LORA_ADVERTISEMENT_PATH, "r2cloud", "peripheral", LORA_SERVICE_UUID);
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
		for (BleService cur : application.getServices()) {
			for (BleCharacteristic curChar : cur.getCharacteristics()) {
				dbusConn.exportObject(curChar);
				dbusConn.exportObject(curChar.getDescriptor());
			}
			dbusConn.exportObject(cur);
		}
		dbusConn.exportObject(application);
	}

	private static void unExportAll(DBusConnection dbusConn, BleApplication application) {
		for (BleService cur : application.getServices()) {
			for (BleCharacteristic curChar : cur.getCharacteristics()) {
				dbusConn.unExportObject(curChar.getDescriptor().getObjectPath());
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
