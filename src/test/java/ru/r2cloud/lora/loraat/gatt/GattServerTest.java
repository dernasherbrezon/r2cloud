package ru.r2cloud.lora.loraat.gatt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bluez.GattCharacteristic1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.DefaultClock;

public class GattServerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private BluezServer bluezServer;
	private GattServer gattServer;
	private LoraAtBleDevice device;
	private String bluetoothAddress = "78:DD:08:A3:A7:52";

	@Test
	public void testSuccess() throws Exception {
		List<ApplicationInfo> applications = bluezServer.getManager().getRegisteredApplications();
		assertEquals(1, applications.size());
		ApplicationInfo app = applications.get(0);
		ObjectManager application = bluezServer.getRemoteObject(app.getSource(), app.getObjectPath(), ObjectManager.class);

		byte bluetoothSignalLevel = -34;
		writeValue(app.getSource(), application, new byte[] { (byte) 255, bluetoothSignalLevel }, "5b53256e-76d2-4259-b3aa-15b5b4cfdd32");
		DeviceStatus status = device.getStatus();
		assertNull(status.getBatteryLevel());
		assertNotNull(status.getSignalLevel());
		assertEquals(bluetoothSignalLevel, status.getSignalLevel().intValue());

		LoraFrame frame = createFrame();
		writeValue(app.getSource(), application, serialize(frame), "40d6f70c-5e28-4da4-a99e-c5298d1613fe");
		List<LoraFrame> frames = device.getFrames();
		assertEquals(1, frames.size());
		assertFrameEquals(frame, frames.get(0));
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		device = new LoraAtBleDevice(Configuration.LORA_AT_DEVICE_PREFIX + bluetoothAddress.toLowerCase(Locale.UK), null, 1, null, null, null, new DeviceConfiguration(), null, null, null, null, config);
		DeviceManager manager = new DeviceManager(config, null, null, null);
		manager.addDevice(device);
		String unixFile = "/tmp/system_dbus_r2cloud_test_" + Math.abs(new Random().nextInt());
		bluezServer = new BluezServer(unixFile);
		bluezServer.start();
		gattServer = new GattServer(manager, BusAddress.of("unix:path=" + unixFile), new DefaultClock());
		gattServer.start();
	}

	@After
	public void stop() {
		if (gattServer != null) {
			gattServer.stop();
		}
		if (bluezServer != null) {
			bluezServer.stop();
		}
	}
	
	private static void assertFrameEquals(LoraFrame expected, LoraFrame actual) {
		assertEquals(expected.getFrequencyError(), actual.getFrequencyError());
		assertEquals(expected.getRssi(), actual.getRssi());
		assertEquals(expected.getSnr(), actual.getSnr(), 0.0f);
		assertEquals(expected.getTimestamp(), actual.getTimestamp());
		assertArrayEquals(expected.getData(), actual.getData());
	}

	private static LoraFrame createFrame() {
		LoraFrame result = new LoraFrame();
		result.setFrequencyError(-6543);
		result.setRssi((short) -101);
		result.setSnr(7.2f);
		result.setTimestamp(System.currentTimeMillis());
		result.setData(new byte[] { (byte) 0xCA, (byte) 0xFE });
		return result;
	}

	private static byte[] serialize(LoraFrame frame) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt((int) frame.getFrequencyError());
		dos.writeShort(frame.getRssi());
		dos.writeFloat(frame.getSnr());
		dos.writeLong(frame.getTimestamp());
		dos.writeInt(frame.getData().length);
		dos.write(frame.getData());
		return baos.toByteArray();
	}

	private void writeValue(String source, ObjectManager application, byte[] value, String statusUuid) throws DBusException, BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		DBusPath statusPath = getCharacteristic1(statusUuid, application);
		assertNotNull(statusPath);
		GattCharacteristic1 status = bluezServer.getRemoteObject(source, statusPath.getPath(), GattCharacteristic1.class);
		assertNotNull(status);
		HashMap<String, Variant<?>> options = new HashMap<>();
		options.put("device", new Variant<>("/dev_" + bluetoothAddress.replace(':', '_')));
		status.WriteValue(value, options);
	}

	private static DBusPath getCharacteristic1(String uuid, ObjectManager adapter) {
		for (Entry<DBusPath, Map<String, Map<String, Variant<?>>>> cur : adapter.GetManagedObjects().entrySet()) {
			if (!cur.getValue().containsKey("org.bluez.GattCharacteristic1")) {
				continue;
			}
			Map<String, Variant<?>> properties = cur.getValue().get("org.bluez.GattCharacteristic1");
			for (Entry<String, Variant<?>> curProperty : properties.entrySet()) {
				if (curProperty.getKey().equals("UUID") && curProperty.getValue().getValue().toString().equals(uuid)) {
					return cur.getKey();
				}
			}
		}
		return null;
	}

}
