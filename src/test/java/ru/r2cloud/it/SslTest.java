package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class SslTest extends RegisteredTest {

	@Test
	public void testGetSettings() {
		TestUtil.assertJson("emptySslConfig.json", client.getSsl());
	}

	@Test
	public void testEmptyDomain() {
		HttpResponse<String> response = client.saveSslConfigurationResponse(true, true, null);
		assertEquals(400, response.statusCode());
		assertErrorInField("domain", response);
	}

	@Test
	public void testNotAgreedWithToC() {
		HttpResponse<String> response = client.saveSslConfigurationResponse(true, false, "https://example.com");
		assertEquals(400, response.statusCode());
		assertErrorInField("agreeWithToC", response);
	}

}
