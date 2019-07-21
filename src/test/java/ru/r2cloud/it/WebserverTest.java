package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.BaseTest;

public class WebserverTest extends BaseTest {

	@Test
	public void testNotFound() {
		HttpResponse<String> response = client.getFileResponse("/" + UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}

	@Test
	public void testNonAuth() {
		HttpResponse<String> response = client.saveR2CloudConfigurationWithResponse(UUID.randomUUID().toString(), true);
		assertEquals(401, response.statusCode());
	}
	
	@Test
	public void testInvalidPost() {
		HttpResponse<String> response = client.postData("/api/v1/setup/setup", UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
	}
	
	@Test
	public void testInvalidPostBody() {
		HttpResponse<String> response = client.postData("/api/v1/setup/setup", "[{\"test\": 1 }]");
		assertEquals(400, response.statusCode());
	}

	@Test
	public void testOptions() {
		HttpResponse<String> response = client.getOptions("/");
		assertEquals(200, response.statusCode());
		assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").get());
		assertEquals("1728000", response.headers().firstValue("Access-Control-Max-Age").get());
		assertEquals("GET, POST, OPTIONS", response.headers().firstValue("Access-Control-Allow-Methods").get());
		assertEquals("Authorization, Content-Type", response.headers().firstValue("Access-Control-Allow-Headers").get());
		assertEquals("Authorization, Content-Type", response.headers().firstValue("Access-Control-Expose-Headers").get());
	}

}
