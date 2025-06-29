package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.CollectingRequestHandler;
import ru.r2cloud.RotctrldMock;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.util.Configuration;

public class SharedRotatorTest extends RegisteredTest {

	private RotctrldMock rotctrlMock;

	@Test
	public void testSuccess() throws Exception {
		JsonArray schedule = client.getFullSchedule();
		try (Reader is = new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream("expected/sharedRotatorSchedule.json"), StandardCharsets.UTF_8)) {
			JsonValue value = Json.parse(is);
			assertTrue(value.isArray());
			JsonArray expected = value.asArray();
			assertEquals(expected.size(), schedule.size());
			for (int i = 0; i < expected.size(); i++) {
				JsonObject expectedObj = expected.get(i).asObject();
				JsonObject actualObj = schedule.get(i).asObject();
				assertEquals(expectedObj.get("id").asString(), actualObj.get("id").asString());
				assertEquals(expectedObj.get("satelliteId").asString(), actualObj.get("satelliteId").asString());
				assertTimestamps(expectedObj.get("start").asLong(), actualObj.get("start").asLong());
				assertTimestamps(expectedObj.get("end").asLong(), actualObj.get("end").asLong());
			}
		}
	}

	@Override
	protected Configuration prepareConfiguration() throws IOException {
		Configuration result = super.prepareConfiguration();
		result.setProperty("satellites.meta.location", "./src/test/resources/satellites-test-schedule.json");
		result.setProperty("rtlsdr.devices", "0,1");
		result.setProperty("rtlsdr.device.0.rotctrld.hostname", "127.0.0.1");
		result.setProperty("rtlsdr.device.0.rotctrld.port", "8004");
		result.setProperty("rtlsdr.device.0.rotator.tolerance", 5);
		result.setProperty("rtlsdr.device.0.rotator.cycleMillis", 1000);
		result.setProperty("rtlsdr.device.0.minFrequency", 433000000);
		result.setProperty("rtlsdr.device.0.maxFrequency", 480000000);

		result.setProperty("rtlsdr.device.1.rotctrld.hostname", "127.0.0.1");
		result.setProperty("rtlsdr.device.1.rotctrld.port", "8004");
		result.setProperty("rtlsdr.device.1.rotator.tolerance", 5);
		result.setProperty("rtlsdr.device.1.rotator.cycleMillis", 1000);
		result.setProperty("rtlsdr.device.1.minFrequency", 433000000);
		result.setProperty("rtlsdr.device.1.maxFrequency", 480000000);

		return result;
	}

	@Override
	public void start() throws Exception {
		rotctrlMock = new RotctrldMock(8004);
		rotctrlMock.setHandler(new CollectingRequestHandler("RPRT 0\n"));
		rotctrlMock.start();
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		if (rotctrlMock != null) {
			rotctrlMock.stop();
		}
	}

	private static void assertTimestamps(long expected, long actual) {
		long expectedSeconds = (long) (Math.floor(expected / 1000.0f));
		long actualSeconds = (long) (Math.floor(actual / 1000.0f));
		assertEquals(expectedSeconds, actualSeconds);
	}
}
