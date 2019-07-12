package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class DdnsTest extends RegisteredTest {

	@Test
	public void testGetSettings() {
		TestUtil.assertJson("noDdns.json", client.getDdnsConfiguration());
	}

	@Test
	public void testSaveNONE() {
		client.saveDdnsConfiguration("NONE", null, null, null);
	}

	@Test
	public void testEmptyUsername() {
		HttpResponse<String> response = client.saveDdnsConfigurationResponse("NOIP", null, UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
		assertErrorInField("username", response);
	}

	@Test
	public void testEmptyPassword() {
		HttpResponse<String> response = client.saveDdnsConfigurationResponse("NOIP", UUID.randomUUID().toString(), null, UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
		assertErrorInField("password", response);
	}

	@Test
	public void testEmptyDomain() {
		HttpResponse<String> response = client.saveDdnsConfigurationResponse("NOIP", UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
		assertEquals(400, response.statusCode());
		assertErrorInField("domain", response);
	}

	@Test
	public void testSaveUnknownType() {
		HttpResponse<String> response = client.saveDdnsConfigurationResponse(UUID.randomUUID().toString(), null, null, null);
		assertEquals(400, response.statusCode());
		assertErrorInField("type", response);
	}

	@Test
	public void testEmptyType() {
		HttpResponse<String> response = client.saveDdnsConfigurationResponse(null, null, null, null);
		assertEquals(400, response.statusCode());
		assertErrorInField("type", response);
	}
}
