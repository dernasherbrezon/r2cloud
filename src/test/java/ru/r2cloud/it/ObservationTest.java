package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.R2CloudServer;
import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationTest extends RegisteredTest {

	private final static String METEOR_ID = "40069";

	private RtlSdrDataServer rtlSdrMock;
	private R2CloudServer server;

	@Test
	public void testMeteorObservation() throws Exception {
		rtlSdrMock.mockResponse("/data/40069-1553411549943.raw.gz");
		JsonHttpResponse spectogramHandler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(1L, spectogramHandler);
		JsonHttpResponse metaHandler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(metaHandler);

		// start observation
		String observationId = client.scheduleStart(METEOR_ID);
		assertNotNull(observationId);
		// get observation and assert
		assertObservation(awaitObservation(observationId));

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

	private static void assertObservation(JsonObject observation) {
		assertNotNull(observation);
		assertEquals(144000, observation.getInt("sampleRate", 0));
		assertEquals(288000, observation.getInt("inputSampleRate", 0));
		assertEquals(137100000, observation.getInt("frequency", 0));
		assertEquals(137100000, observation.getInt("actualFrequency", 0));
		// do not assert numberOfDecodedPackets as file contains valid data
		// there is no control on when test executed, so doppler correction might
		// generate valid freq offset and numberOfDecodedPackets might NOT be 0
		// assertEquals(0, observation.getInt("numberOfDecodedPackets", -1));
		assertEquals("LRPT", observation.getString("decoder", null));
		assertEquals(METEOR_ID, observation.getString("satellite", null));
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
		client.saveR2CloudConfiguration(UUID.randomUUID().toString(), true);
		rtlSdrMock = new RtlSdrDataServer();
		rtlSdrMock.start();
		server = new R2CloudServer();
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

	private JsonObject awaitObservation(String observationId) {
		// experimental. 20 seconds to process LRPT
		int maxRetries = 40;
		int curRetry = 0;
		while (!Thread.currentThread().isInterrupted() && curRetry < maxRetries) {
			JsonObject observation = client.getObservation(METEOR_ID, observationId);
			if (observation != null && observation.get("numberOfDecodedPackets") != null) {
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
