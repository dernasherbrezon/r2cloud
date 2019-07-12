package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleCompleteTest extends RegisteredTest {

	@Test
	public void testMissingId() {
		HttpResponse<String> response = client.scheduleCompleteResponse(null);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}

	@Test
	public void testUnknownId() {
		HttpResponse<String> response = client.scheduleCompleteResponse(UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}

}
