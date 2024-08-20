package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.LeoSatDataServerMock;
import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.model.IntegrationConfiguration;

public class ObservationTest extends RegisteredTest {

	private RtlSdrDataServer rtlSdrMock;
	private LeoSatDataServerMock server;

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
		TestUtil.assertObservation("r2cloudclienttest/46494-0.json", client.awaitObservation(satelliteId, observationIds.get(0), false));
		TestUtil.assertObservation("r2cloudclienttest/46494-1.json", client.awaitObservation(satelliteId, observationIds.get(1), false));
	}

	static void assertSpectogram(String expectedFilename, byte[] actualBytes) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(actualBytes)) {
			TestUtil.assertImage(expectedFilename, bais);
		}
	}

	@Before
	@Override
	public void start() throws Exception {
		super.start();
		IntegrationConfiguration integrations = new IntegrationConfiguration();
		integrations.setApiKey(UUID.randomUUID().toString());
		integrations.setNewLaunch(true);
		integrations.setSyncSpectogram(true);
		integrations.setSatnogs(false);
		client.saveIntegrationConfiguration(integrations);
		rtlSdrMock = new RtlSdrDataServer();
		rtlSdrMock.start();
		server = new LeoSatDataServerMock();
		server.start();
	}

	@After
	@Override
	public void stop() {
		if (server != null) {
			server.stop();
		}
		if (rtlSdrMock != null) {
			rtlSdrMock.stop();
		}
		super.stop();
	}

}
