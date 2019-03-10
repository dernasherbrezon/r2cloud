package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.RegisteredTest;

public class AccessTokenIT extends RegisteredTest {

	@Test
	public void testInvalidUsername() {
		HttpResponse<String> response = client.loginWithResponse(null, password);
		assertEquals(401, response.statusCode());
	}

	@Test
	public void testInvalidPassword() {
		HttpResponse<String> response = client.loginWithResponse(username, null);
		assertEquals(401, response.statusCode());
	}

	@Test
	public void testUnknownUsername() {
		HttpResponse<String> response = client.loginWithResponse(UUID.randomUUID().toString(), password);
		assertEquals(401, response.statusCode());
	}
}
