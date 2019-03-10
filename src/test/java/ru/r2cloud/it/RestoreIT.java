package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.RegisteredTest;

public class RestoreIT extends RegisteredTest {

	@Test
	public void testReset() {
		client.resetPassword(username);
		HttpResponse<String> response = client.loginWithResponse(username, password);
		assertEquals(401, response.statusCode());
	}

	@Test
	public void testInvalidUsername() {
		HttpResponse<String> response = client.resetPasswordWithResponse(null);
		assertEquals(400, response.statusCode());
		assertErrorInField("username", response);
	}

	@Test
	public void testUnknownUsername() {
		client.resetPassword(UUID.randomUUID().toString());
		client.login(username, password);
	}

}
