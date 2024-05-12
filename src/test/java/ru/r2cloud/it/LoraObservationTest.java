package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.loraat.gatt.BluezServer;
import ru.r2cloud.lora.loraat.gatt.GattClientTest;
import ru.r2cloud.lora.loraat.gatt.MockGattServer;
import ru.r2cloud.util.Configuration;

public class LoraObservationTest extends RegisteredTest {

	private BluezServer bluezServer;
	private MockGattServer mockServer;

	private String remoteBluetoothAddress = "7b:7a:24:3c:ed:50";

	@Test
	public void testLoraObservation() throws Exception {
		String satelliteId = "59114";
		List<String> observationIds = client.scheduleStart(satelliteId);
		assertEquals(1, observationIds.size());

		mockServer.awaitObservation();

		LoraFrame frame = new LoraFrame();
		frame.setData(new byte[] { (byte) 0xFF, 0x00 });
		frame.setFrequencyError(1234);
		frame.setRssi((short) -23);
		frame.setSnr(-2.34f);
		frame.setTimestamp(1707737548000L);
		mockServer.sendLoraFrame(frame);

		// make sure frame asynchronously received
		GattClientTest.waitForTheFrame(server.gattClient);

		client.scheduleComplete(observationIds.get(0));

		// get observation and assert
		JsonObject actual = client.awaitObservation(satelliteId, observationIds.get(0), true);
		assertNull(actual.get("rawURL"));
		TestUtil.assertObservation("r2cloudclienttest/1559982858904-59114-0.json", actual);
	}

	@Override
	protected Configuration prepareConfiguration() throws IOException {
		Configuration result = super.prepareConfiguration();
		result.setProperty("satellites.meta.location", "./src/test/resources/satellites-lora.json");
		result.setProperty("loraatblec.devices", "0");
		result.setProperty("loraatblec.device.0.btaddress", remoteBluetoothAddress);
		result.setProperty("loraatblec.device.0.minFrequency", 433000000);
		result.setProperty("loraatblec.device.0.maxFrequency", 480000000);

		result.remove("loraatwifi.devices");
		result.remove("loraat.devices");
		result.remove("rtlsdr.devices");
		result.remove("spyserver.devices");
		result.remove("plutosdr.devices");

		return result;
	}

	@Before
	@Override
	public void start() throws Exception {
		bluezServer = new BluezServer(unixFile);
		bluezServer.start();

		mockServer = new MockGattServer(remoteBluetoothAddress, bluezServer);
		mockServer.start();

		super.start();
	}

	@After
	@Override
	public void stop() {
		if (bluezServer != null) {
			bluezServer.stop();
		}
		super.stop();
	}

}
