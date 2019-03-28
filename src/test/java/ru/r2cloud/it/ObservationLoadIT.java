package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.RegisteredTest;

public class ObservationLoadIT extends RegisteredTest {

	@Test
	public void testInvalidArguments() {
		HttpResponse<String> response = client.getObservationResponse(null, UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
		assertErrorInField("satelliteId", response);
	}

	@Test
	public void testInvalidArguments2() {
		HttpResponse<String> response = client.getObservationResponse(UUID.randomUUID().toString(), null);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}

	@Test
	public void testUnknownObservation() {
		HttpResponse<String> response = client.getObservationResponse(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}

}
