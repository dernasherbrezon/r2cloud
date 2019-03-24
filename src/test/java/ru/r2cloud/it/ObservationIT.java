package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.RtlSdrDataServer;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationIT extends RegisteredTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private RtlSdrDataServer rtlSdrMock;

	@Test
	public void testMeteorObservation() throws Exception {
		rtlSdrMock.mockResponse("/data/40069-1553411549943.raw");
		String observationId = client.scheduleStart("40069");
		assertNotNull(observationId);
		Thread.sleep(1000);
		client.scheduleComplete("40069");
		JsonObject observation = awaitObservation(observationId);
		assertNotNull(observation);
		assertEquals(150000, observation.getInt("sampleRate", 0));
		assertEquals(240000, observation.getInt("inputSampleRate", 0));
		assertEquals(137900000, observation.getInt("frequency", 0));
		assertEquals(137900000, observation.getInt("actualFrequency", 0));
		assertEquals(0, observation.getInt("numberOfDecodedPackets", -1));
		assertEquals("LRPT", observation.getString("decoder", null));
		assertEquals("40069", observation.getString("satellite", null));
	}

	@Before
	@Override
	public void start() throws Exception {
		super.start();
		rtlSdrMock = new RtlSdrDataServer();
		rtlSdrMock.start();
	}

	@After
	public void stop() {
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
