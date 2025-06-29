package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.LeoSatDataServerMock;
import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.model.IntegrationConfiguration;
import ru.r2cloud.util.Configuration;

public class ObservationTest extends RegisteredTest {

	private RtlSdrDataServer rtlSdrMock;
	private LeoSatDataServerMock server;
	private HttpServer loraAtWifiServer;

	@Test
	public void testMeteorObservation() throws Exception {
		rtlSdrMock.mockResponse("/data/40069-1553411549943.raw.gz");
		JsonHttpResponse metaHandler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(metaHandler);
		JsonHttpResponse spectogramHandler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(1L, spectogramHandler);

		// start observation
		String satelliteId = "40069";
		List<String> observationIds = client.scheduleStart(satelliteId);
		assertEquals(1, observationIds.size());
		// get observation and assert
		TestUtil.assertObservation("r2cloudclienttest/40069-1553411549943-request.json", client.awaitObservation(satelliteId, observationIds.get(0), true));

		// wait for r2cloud meta upload and assert
		metaHandler.awaitRequest();
		assertNotNull(metaHandler.getRequest());
		TestUtil.assertObservation("r2cloudclienttest/40069-1553411549943-request.json", Json.parse(metaHandler.getRequest()).asObject());

		// wait for spectogram upload and assert
		spectogramHandler.awaitRequest();
		assertEquals("image/png", spectogramHandler.getRequestContentType());
		assertSpectogram("spectogram-output.raw.gz.png", spectogramHandler.getRequestBytes());
		assertTempEmpty();
	}

	@Test
	public void testTwoTransmittors() throws Exception {
		JsonHttpResponse metaHandler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(metaHandler);
		JsonHttpResponse spectogramHandler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(1L, spectogramHandler);

		loraAtWifiServer.createContext("/api/v2/lora/rx/start", new JsonHttpResponse("loraatwifitest/success.json", 200));
		loraAtWifiServer.createContext("/api/v2/rx/stop", new JsonHttpResponse("loraatwifitest/successStop.json", 200));

		// start observation
		String satelliteId = "46494";
		List<String> observationIds = client.scheduleStart(satelliteId);
		assertEquals(2, observationIds.size());

		// get observation and assert
		TestUtil.assertObservation("r2cloudclienttest/46494-0.json", client.awaitObservation(satelliteId, observationIds.get(1), false));
		TestUtil.assertObservation("r2cloudclienttest/46494-1.json", client.awaitObservation(satelliteId, observationIds.get(0), false));
	}

	static void assertSpectogram(String expectedFilename, byte[] actualBytes) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(actualBytes)) {
			TestUtil.assertImage(expectedFilename, bais);
		}
	}

	@Override
	protected Configuration prepareConfiguration() throws IOException {
		Configuration result = super.prepareConfiguration();
		result.setProperty("loraatwifi.devices", "0");
		result.setProperty("loraatwifi.device.0.host", "127.0.0.1");
		result.setProperty("loraatwifi.device.0.port", "8005");
		return result;
	}

	@Before
	@Override
	public void start() throws Exception {
		rtlSdrMock = new RtlSdrDataServer();
		rtlSdrMock.start();
		server = new LeoSatDataServerMock();
		server.start();
		loraAtWifiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8005), 0);
		loraAtWifiServer.createContext("/api/v2/status", new JsonHttpResponse("loraatwifitest/status.json", 200));
		loraAtWifiServer.start();
		super.start();
		IntegrationConfiguration integrations = new IntegrationConfiguration();
		integrations.setApiKey(UUID.randomUUID().toString());
		integrations.setNewLaunch(true);
		integrations.setSyncSpectogram(true);
		integrations.setSatnogs(false);
		client.saveIntegrationConfiguration(integrations);
	}

	@After
	@Override
	public void stop() {
		super.stop();
		if (server != null) {
			server.stop();
		}
		if (rtlSdrMock != null) {
			rtlSdrMock.stop();
		}
		if (loraAtWifiServer != null) {
			loraAtWifiServer.stop(0);
		}
	}

}
