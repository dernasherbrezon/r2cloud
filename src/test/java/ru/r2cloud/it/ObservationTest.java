package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.LeoSatDataServerMock;
import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationTest extends RegisteredTest {

	private final static String METEOR_ID = "40069";

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
		List<String> observationIds = client.scheduleStart(METEOR_ID);
		assertEquals(1, observationIds.size());
		// get observation and assert
		assertObservation(awaitObservation(METEOR_ID, observationIds.get(0), true));

		// wait for r2cloud meta upload and assert
		metaHandler.awaitRequest();
		JsonObject actual = null;
		try {
			actual = (JsonObject) Json.parse(metaHandler.getRequest());
		} catch (ParseException e) {
			fail("unable to parse request: " + metaHandler.getRequest() + " content-type: " + metaHandler.getRequestContentType() + " " + e.getMessage());
		}
		assertObservation(actual);

		// wait for spectogram upload and assert
		spectogramHandler.awaitRequest();
		assertEquals("image/png", spectogramHandler.getRequestContentType());
		assertSpectogram("spectogram-output.raw.gz.png", spectogramHandler.getRequestBytes());
		assertTempEmpty();
	}

	@Test
	public void testTwoTransmittors() throws Exception {
		rtlSdrMock.mockResponse("/data/40069-1553411549943.raw.gz");
		JsonHttpResponse metaHandler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(metaHandler);
		JsonHttpResponse spectogramHandler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(1L, spectogramHandler);

		r2loraServer.createContext("/lora/rx/start", new JsonHttpResponse("r2loratest/success.json", 200));
		r2loraServer.createContext("/rx/stop", new JsonHttpResponse("r2loratest/successStop.json", 200));

		// start observation
		String satelliteId = "46494";
		List<String> observationIds = client.scheduleStart(satelliteId);
		assertEquals(2, observationIds.size());

		// get observation and assert
		assertObservation(satelliteId, awaitObservation(satelliteId, observationIds.get(0), false));
		assertObservation(satelliteId, awaitObservation(satelliteId, observationIds.get(1), false));
	}

	private static void assertObservation(JsonObject observation) {
		assertObservation(METEOR_ID, observation);
		assertEquals(288000, observation.getInt("sampleRate", 0));
		assertEquals(137900000, observation.getInt("actualFrequency", 0));
	}

	private static void assertObservation(String satelliteId, JsonObject observation) {
		assertNotNull(observation);
		assertEquals(satelliteId, observation.getString("satellite", null));
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
		client.saveR2CloudConfiguration(UUID.randomUUID().toString(), true, true);
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

	private JsonObject awaitObservation(String satelliteId, String observationId, boolean waitForDecodedPackets) {
		// experimental. 20 seconds to process LRPT
		int maxRetries = 40;
		int curRetry = 0;
		while (!Thread.currentThread().isInterrupted() && curRetry < maxRetries) {
			JsonObject observation = client.getObservation(satelliteId, observationId);
			if (observation != null && (!waitForDecodedPackets || observation.get("numberOfDecodedPackets") != null)) {
				return observation;
			}
			try {
				Thread.sleep(1000);
				curRetry++;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return null;
	}
}
