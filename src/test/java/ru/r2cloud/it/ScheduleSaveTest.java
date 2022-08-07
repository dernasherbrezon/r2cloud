package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleSaveTest extends RegisteredTest {

	@Test
	public void testUpdateConfiguration() {
		// PEGASUS not enabled by default
		String satelliteId = "42784";
		client.updateSchedule(satelliteId, true);
		JsonObject result = client.updateSchedule(satelliteId, true);
		assertNotNull(result.get("nextPass"));
		assertTrue(result.getBoolean("enabled", false));
		result = client.updateSchedule(satelliteId, false);
		assertNull(result.get("nextPass"));
	}

	@Test
	public void testSaveUnknownSatellite() {
		HttpResponse<String> response = client.updateScheduleWithResponse(UUID.randomUUID().toString(), true);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}
	
	@Test
	public void testSaveUnknownSatellite2() {
		HttpResponse<String> response = client.updateScheduleWithResponse(null, true);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}

}
