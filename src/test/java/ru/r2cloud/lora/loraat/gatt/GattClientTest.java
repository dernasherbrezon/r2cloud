package ru.r2cloud.lora.loraat.gatt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Random;

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
	private GattClient client;
	private MockGattServer mockServer;

	private String remoteBluetoothAddress = "7b:7a:24:3c:ed:50";

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

		mockServer.sendLoraFrame(frame);

		waitForTheFrame(client);

		response = client.stopObservation(remoteBluetoothAddress);
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals(1, response.getFrames().size());
		assertLoraFrame(frame, response.getFrames().get(0));
	}

	@Before
	public void start() throws Exception {
		String unixFile = "/tmp/system_dbus_r2cloud_test_" + Math.abs(new Random().nextInt());
		bluezServer = new BluezServer(unixFile);
		bluezServer.start();

		mockServer = new MockGattServer(remoteBluetoothAddress, bluezServer);
		mockServer.start();

		client = new GattClient("unix:path=" + unixFile, new DefaultClock(), 10000);
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

	public static void waitForTheFrame(GattClient client) {
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
		throw new RuntimeException("timeout waiting for the frame");
	}

}
