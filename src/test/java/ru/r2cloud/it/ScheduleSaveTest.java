package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleSaveTest extends RegisteredTest {

	@Test
	public void testUpdateConfiguration() {
		// PEGASUS not enabled by default
		String satelliteId = "42784";
		client.updateSchedule(satelliteId, true);
		TestUtil.assertJson("expected/enableSatelliteResponse.json", client.updateSchedule(satelliteId, true));
		TestUtil.assertJson("expected/disableSatelliteResponse.json", client.updateSchedule(satelliteId, false));
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
