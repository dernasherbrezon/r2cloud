package ru.r2cloud.lora.loraat.gatt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

import ru.r2cloud.FixedClock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.jradio.norbi.NorbiBeacon;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterStatus;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.LoraTransmitterFilter;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class GattServerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private BluezServer bluezServer;
	private GattServer gattServer;
	private LoraAtBleDevice device;
	private String bluetoothAddress;
	private long currentTime = 1559942730784L;
	private int gain = 0;

	@Test
	public void testSuccess() throws Exception {
		List<ApplicationInfo> applications = bluezServer.getManager().getRegisteredApplications();
		assertEquals(1, applications.size());
		ApplicationInfo app = applications.get(0);
		ObjectManager application = bluezServer.getRemoteObject(app.getSource(), app.getObjectPath(), ObjectManager.class);

		byte bluetoothSignalLevel = -34;
		writeValue(app.getSource(), application, new byte[] { (byte) 255, bluetoothSignalLevel }, GattServer.STATUS_CHARACTERISTIC_UUID);
		DeviceStatus status = device.getStatus();
		assertNull(status.getBatteryLevel());
		assertNotNull(status.getSignalLevel());
		assertEquals(bluetoothSignalLevel, status.getSignalLevel().intValue());

		LoraFrame frame = createFrame();
		writeValue(app.getSource(), application, serialize(frame), GattServer.SCHEDULE_CHARACTERISTIC_UUID);
		List<LoraFrame> frames = device.getFrames();
		assertEquals(1, frames.size());
		assertFrameEquals(frame, frames.get(0));

		byte[] empty = readValue(app.getSource(), application, GattServer.SCHEDULE_CHARACTERISTIC_UUID);
		assertEquals(0, empty.length);

		Transmitter transmitter = createTransmitter();
		device.tryTransmitter(transmitter);
		device.reschedule();

		assertLoraRequest(transmitter, 1559957416215L, 1559957939690L, currentTime, deserialize(readValue(app.getSource(), application, GattServer.SCHEDULE_CHARACTERISTIC_UUID)));
	}

	@Test
	public void testInvalidArguments() throws Exception {
		List<ApplicationInfo> applications = bluezServer.getManager().getRegisteredApplications();
		assertEquals(1, applications.size());
		ApplicationInfo app = applications.get(0);
		ObjectManager application = bluezServer.getRemoteObject(app.getSource(), app.getObjectPath(), ObjectManager.class);

		writeValue(app.getSource(), application, new byte[] { (byte) 24 }, GattServer.STATUS_CHARACTERISTIC_UUID);
		assertNull(device.getStatus().getBatteryLevel());

		writeValue(app.getSource(), application, new byte[] { (byte) 24 }, GattServer.SCHEDULE_CHARACTERISTIC_UUID);
		assertTrue(device.getFrames().isEmpty());

		// simulate unknown device
		bluetoothAddress = "00:DD:08:A3:A7:00";
		writeValue(app.getSource(), application, new byte[] { (byte) 24, 33 }, GattServer.STATUS_CHARACTERISTIC_UUID);
		assertNull(device.getStatus().getBatteryLevel());

		byte[] empty = readValue(app.getSource(), application, GattServer.SCHEDULE_CHARACTERISTIC_UUID);
		assertEquals(0, empty.length);
		
		LoraFrame frame = createFrame();
		writeValue(app.getSource(), application, serialize(frame), GattServer.SCHEDULE_CHARACTERISTIC_UUID);
		assertTrue(device.getFrames().isEmpty());
	}

	@Before
	public void start() throws Exception {
		bluetoothAddress = "78:DD:08:A3:A7:52";
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("loraatble.devices", "0");
		config.setProperty("loraatble.timeout", 10000);
		config.setProperty("loraatble.device.0.gain", gain);
		config.setProperty("loraatble.device.0.btaddress", bluetoothAddress.toLowerCase(Locale.UK));
		config.setProperty("loraatble.device.0.minFrequency", 400_000_000);
		config.setProperty("loraatble.device.0.maxFrequency", 500_000_000);
		DeviceConfiguration deviceConfiguration = config.getLoraAtBleConfigurations().get(0);
		PredictOreKit predict = new PredictOreKit(config);
		ObservationFactory factory = new ObservationFactory(predict);
		SatelliteDao satelliteDao = new SatelliteDao(config);
		ThreadPoolFactoryImpl threadFactory = new ThreadPoolFactoryImpl(60000);
		Clock clock = new FixedClock(currentTime);

		device = new LoraAtBleDevice(deviceConfiguration.getId(), new LoraTransmitterFilter(deviceConfiguration), 1, factory, threadFactory, clock, deviceConfiguration, null, null, predict, null, config);
		DeviceManager manager = new DeviceManager(config, satelliteDao, threadFactory, clock);
		manager.addDevice(device);
		String unixFile = "/tmp/system_dbus_r2cloud_test_" + Math.abs(new Random().nextInt());
		bluezServer = new BluezServer(unixFile);
		bluezServer.start();
		gattServer = new GattServer(manager, BusAddress.of("unix:path=" + unixFile), clock);
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

	private static Transmitter createTransmitter() {
		Transmitter result = new Transmitter();
		result.setModulation(Modulation.LORA);
		result.setFraming(Framing.LORA);
		result.setBeaconClass(NorbiBeacon.class);
		result.setFrequency(436703000L);
		result.setBandwidth(0);
		result.setStatus(TransmitterStatus.ENABLED);
		result.setLoraBandwidth(250_000L);
		result.setLoraSpreadFactor(10);
		result.setLoraCodingRate(5);
		result.setLoraSyncword(18);
		result.setLoraPreambleLength(8);
		result.setLoraLdro(0);
		result.setTle(new Tle(new String[] { "NORBI", "1 46494U 20068J   22336.90274690  .00003263  00000+0  22606-3 0  9998", "2 46494  97.7471 278.8262 0018953  74.3629 285.9691 15.06230312119549" }));
		result.setEnabled(true);
		result.setFrequencyBand(result.getFrequency());
		result.setPriority(Priority.NORMAL);
		result.setId("46494-1");
		return result;
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
		dos.writeByte(frame.getData().length);
		dos.write(frame.getData());
		return baos.toByteArray();
	}

	private void assertLoraRequest(Transmitter transmitter, long startTime, long endTime, long time, LoraBleObservationRequest actual) {
		assertEquals(startTime, actual.getStartTimeMillis());
		assertEquals(endTime, actual.getEndTimeMillis());
		assertEquals(time, actual.getCurrentTime());
		assertEquals(transmitter.getFrequency() / 1_000_000.0f, actual.getFrequency(), 0.0f);
		assertEquals(transmitter.getLoraBandwidth() / 1_000.0f, actual.getLoraBandwidth(), 0.0f);
		assertEquals(transmitter.getLoraSpreadFactor(), actual.getLoraSpreadFactor());
		assertEquals(transmitter.getLoraCodingRate(), actual.getLoraCodingRate());
		assertEquals(transmitter.getLoraSyncword(), actual.getLoraSyncword());
		// 10 is hardcoded
		assertEquals(10, actual.getPower());
		assertEquals(transmitter.getLoraPreambleLength(), actual.getLoraPreambleLength());
		assertEquals(gain, actual.getGain());
		assertEquals(transmitter.getLoraLdro(), actual.getLoraLdro());
		assertEquals(transmitter.isLoraCrc() ? 1 : 0, actual.getLoraCrc());
		assertEquals(transmitter.isLoraExplicitHeader() ? 1 : 0, actual.getLoraExplicitHeader());
		assertEquals(transmitter.getBeaconSizeBytes(), actual.getBeaconSizeBytes());
	}

	private static LoraBleObservationRequest deserialize(byte[] readValue) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(readValue));
		LoraBleObservationRequest result = new LoraBleObservationRequest();
		result.setStartTimeMillis(dis.readLong());
		result.setEndTimeMillis(dis.readLong());
		result.setCurrentTime(dis.readLong());
		result.setFrequency(dis.readFloat());
		result.setLoraBandwidth(dis.readFloat());
		result.setLoraSpreadFactor(dis.readUnsignedByte());
		result.setLoraCodingRate(dis.readUnsignedByte());
		result.setLoraSyncword(dis.readUnsignedByte());
		result.setPower(dis.readUnsignedByte());
		result.setLoraPreambleLength(dis.readUnsignedShort());
		result.setGain(dis.readUnsignedByte());
		result.setLoraLdro(dis.readUnsignedByte());
		result.setLoraCrc(dis.readUnsignedByte());
		result.setLoraExplicitHeader(dis.readUnsignedByte());
		result.setBeaconSizeBytes(dis.readUnsignedByte());
		return result;
	}

	private void writeValue(String source, ObjectManager application, byte[] value, String uuid) throws DBusException, BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		DBusPath statusPath = getCharacteristic1(uuid, application);
		assertNotNull(statusPath);
		GattCharacteristic1 status = bluezServer.getRemoteObject(source, statusPath.getPath(), GattCharacteristic1.class);
		assertNotNull(status);
		HashMap<String, Variant<?>> options = new HashMap<>();
		options.put("device", new Variant<>("/dev_" + bluetoothAddress.replace(':', '_')));
		status.WriteValue(value, options);
	}

	private byte[] readValue(String source, ObjectManager application, String uuid) throws DBusException, BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException {
		DBusPath statusPath = getCharacteristic1(uuid, application);
		assertNotNull(statusPath);
		GattCharacteristic1 status = bluezServer.getRemoteObject(source, statusPath.getPath(), GattCharacteristic1.class);
		assertNotNull(status);
		HashMap<String, Variant<?>> options = new HashMap<>();
		options.put("device", new Variant<>("/dev_" + bluetoothAddress.replace(':', '_')));
		return status.ReadValue(options);
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
