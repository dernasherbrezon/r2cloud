package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.BaseTest;

public class SetupIT extends BaseTest {

	private String username = "info@r2cloud.ru";
	private String password = "1";
	private String keyword = "ittests";

	@Test
	public void testSetup() {
		client.setup(keyword, username, password);
		client.login(username, password);
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
