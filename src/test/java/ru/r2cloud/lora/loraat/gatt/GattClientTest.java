package ru.r2cloud.lora.loraat.gatt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bluez.GattCharacteristic1;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.DefaultClock;

public class GattClientTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private BluezServer bluezServer;
	private String unixFile;
	private GattClient client;

	private MockBleCharacteristic startRx;
	private MockBleCharacteristic stopRx;
	private MockBleCharacteristic frame;
	private String remoteBluetoothAddress = "7b:7a:24:3c:ed:50";
	private String frameObjectPath;

	@Test
	public void testGetStatus() {
		assertEquals(DeviceConnectionStatus.CONNECTED, client.getStatus(remoteBluetoothAddress));
	}

	@Test
	public void testSuccess() throws Exception {
		LoraObservationRequest req = new LoraObservationRequest();
		req.setFrequency(437200012L);
		req.setBw(125_000);
		req.setSf(9);
		req.setCr(5);
		req.setSyncword(18);
		req.setPreambleLength(8);
		req.setGain(4);
		req.setLdro(0);
		req.setUseCrc(true);
		req.setUseExplicitHeader(true);
		req.setBeaconSizeBytes(0);
		LoraResponse response = client.startObservation(remoteBluetoothAddress, req);
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());

		LoraFrame frame = new LoraFrame();
		frame.setData(new byte[] { (byte) 0xFF, 0x00 });
		frame.setFrequencyError(1234);
		frame.setRssi((short) -23);
		frame.setSnr(-2.34f);
		frame.setTimestamp(1707737548000L);

		Map<String, Variant<?>> propsChanged = new HashMap<>();
		propsChanged.put("Value", new Variant<>(frame.write()));
		bluezServer.sendMessage(new PropertiesChanged(frameObjectPath, GattCharacteristic1.class.getName(), propsChanged, Collections.emptyList()));

		waitForTheFrame();

		response = client.stopObservation(remoteBluetoothAddress);
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals(1, response.getFrames().size());
		assertLoraFrame(frame, response.getFrames().get(0));
	}

	@Before
	public void start() throws Exception {
		String adapterPath = "/org/bluez/hci0";
		String devicePath = adapterPath + "/dev_" + remoteBluetoothAddress.replace(':', '_');
		String servicePrefix = devicePath + "/service001";
		startRx = new MockBleCharacteristic(servicePrefix + "/char001", new String[] { "encrypted-write" }, GattClient.LORA_START_RX_UUID, servicePrefix, null, null);
		stopRx = new MockBleCharacteristic(servicePrefix + "/char002", new String[] { "encrypted-write" }, GattClient.LORA_STOP_RX_UUID, servicePrefix, null, null);
		frameObjectPath = servicePrefix + "/char003";
		frame = new MockBleCharacteristic(frameObjectPath, new String[] { "read" }, GattClient.LORA_FRAME_UUID, servicePrefix, null, null);

		List<BleCharacteristic> chars = new ArrayList<>();
		chars.add(startRx);
		chars.add(stopRx);
		chars.add(frame);

		unixFile = "/tmp/system_dbus_r2cloud_test_" + Math.abs(new Random().nextInt());
		bluezServer = new BluezServer(unixFile);
		bluezServer.start();
		bluezServer.registerBluetoothAdapter(new BluezAdapter(adapterPath, "d8:3a:dd:53:89:ec"));
		bluezServer.registerBluetoothDevice(new BluezDevice(devicePath, remoteBluetoothAddress, new BleService(servicePrefix, GattClient.LORA_SERVICE_UUID, true, chars)));

		client = new GattClient("unix:path=" + unixFile, new DefaultClock());
		client.addDevice(remoteBluetoothAddress);
		client.start();
	}

	@After
	public void stop() {
		if (client != null) {
			client.stop();
		}
		if (bluezServer != null) {
			bluezServer.stop();
		}
	}

	private static void assertLoraFrame(LoraFrame expected, LoraFrame actual) {
		assertArrayEquals(expected.getData(), actual.getData());
		assertEquals(expected.getFrequencyError(), actual.getFrequencyError());
		assertEquals(expected.getRssi(), actual.getRssi());
		assertEquals(expected.getSnr(), actual.getSnr(), 0.0f);
		assertEquals(expected.getTimestamp(), actual.getTimestamp());
	}

	private void waitForTheFrame() {
		long currentWait = 0;
		long totalWait = 10_000;
		long waitPeriod = 100;
		while (currentWait < totalWait) {
			long started = System.currentTimeMillis();
			try {
				Thread.sleep(waitPeriod);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (!client.getReceivedFrames().isEmpty()) {
				return;
			}
			currentWait += System.currentTimeMillis() - started;
		}
	}

}
