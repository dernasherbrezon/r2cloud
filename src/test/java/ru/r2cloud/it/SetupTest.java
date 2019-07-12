package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.BaseTest;

public class SetupTest extends BaseTest {

	@Test
	public void testSetup() {
		client.setup(keyword, username, password);
		HttpResponse<String> response = client.loginWithResponse(username, password);
		assertEquals(200, response.statusCode());
	}

	@Test
	public void testInvalidKeyword() {
		String secondPassword = UUID.randomUUID().toString();
		HttpResponse<String> response = client.setupWithResponse(UUID.randomUUID().toString(), username, secondPassword);
		assertEquals(400, response.statusCode());
		assertErrorInField("keyword", response);
		response = client.loginWithResponse(username, secondPassword);
		assertEquals(401, response.statusCode());
	}

	@Test
	public void testMissingKeywordFile() {
		assertTrue(new File(config.getProperty("server.keyword.location")).delete());
		HttpResponse<String> response = client.setupWithResponse(keyword, username, password);
		assertEquals(400, response.statusCode());
		assertErrorInField("general", response);
	}

	@Test
	public void testEmptyKeyword() {
		HttpResponse<String> response = client.setupWithResponse(null, username, password);
		assertEquals(400, response.statusCode());
		assertErrorInField("keyword", response);
	}

	@Test
	public void testInvalidUsername() {
		HttpResponse<String> response = client.setupWithResponse(keyword, null, password);
		assertEquals(400, response.statusCode());
		assertErrorInField("username", response);
	}

	@Test
	public void testInvalidPassword() {
		HttpResponse<String> response = client.setupWithResponse(keyword, username, null);
		assertEquals(400, response.statusCode());
		assertErrorInField("password", response);
	}
}
