package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.R2CloudServer;
import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationIT extends RegisteredTest {

	private RtlSdrDataServer rtlSdrMock;
	private R2CloudServer server;

	@Test
	public void testMeteorObservation() throws Exception {
		rtlSdrMock.mockResponse("/data/40069-1553411549943.raw");
		JsonHttpResponse metaHandler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(metaHandler);
		JsonHttpResponse spectogramHandler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(1L, spectogramHandler);

		// start observation
		String observationId = client.scheduleStart("40069");
		assertNotNull(observationId);
		Thread.sleep(1000);
		// complete observation
		client.scheduleComplete("40069");

		// get observation and assert
		assertObservation(awaitObservation(observationId));

		// wait for r2cloud meta upload and assert
		metaHandler.awaitRequest();
		assertObservation((JsonObject) Json.parse(metaHandler.getRequest()));

		// wait for spectogram upload and assert
		spectogramHandler.awaitRequest();
		assertEquals("image/png", spectogramHandler.getRequestContentType());
		assertSpectogram("meteor.spectogram.png", spectogramHandler.getRequestBytes());
	}

	private static void assertObservation(JsonObject observation) {
		assertNotNull(observation);
		assertEquals(150000, observation.getInt("sampleRate", 0));
		assertEquals(240000, observation.getInt("inputSampleRate", 0));
		assertEquals(137900000, observation.getInt("frequency", 0));
		assertEquals(137900000, observation.getInt("actualFrequency", 0));
		assertEquals(0, observation.getInt("numberOfDecodedPackets", -1));
		assertEquals("LRPT", observation.getString("decoder", null));
		assertEquals("40069", observation.getString("satellite", null));
	}

	private static void assertSpectogram(String expectedFilename, byte[] actualBytes) throws IOException {
		try (InputStream is1 = ObservationIT.class.getClassLoader().getResourceAsStream(expectedFilename); ByteArrayInputStream bais = new ByteArrayInputStream(actualBytes)) {
			BufferedImage expected = ImageIO.read(is1);
			BufferedImage actual = ImageIO.read(bais);
			for (int i = 0; i < expected.getWidth(); i++) {
				for (int j = 0; j < expected.getHeight(); j++) {
					assertEquals(expected.getRGB(i, j), actual.getRGB(i, j));
				}
			}
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
	public void stop() {
		if (server != null) {
			server.stop();
		}
		if (rtlSdrMock != null) {
			rtlSdrMock.stop();
		}
	}

	private JsonObject awaitObservation(String observationId) {
		// experimental. 20 seconds to process LRPT
		int maxRetries = 40;
		int curRetry = 0;
		while (!Thread.currentThread().isInterrupted() && curRetry < maxRetries) {
			JsonObject observation = client.getObservation("40069", observationId);
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
