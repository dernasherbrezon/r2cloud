package ru.r2cloud.ddns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.MockServer;

public class ExternalIpClientTest {

	private MockServer mockServer;
	private ExternalIpClient client;

	@Test
	public void testSuccess() throws Exception {
		mockServer.mockResponse("/", "1.2.3.4");
		assertEquals("1.2.3.4", client.getExternalIp());
	}

	@Test
	public void testNotFound() throws Exception {
		assertNull(client.getExternalIp());
	}

	@Test
	public void testNoconnection() throws Exception {
		mockServer.stop();
		assertNull(client.getExternalIp());
	}

	@Before
	public void start() throws Exception {
		mockServer = new MockServer();
		mockServer.start();
		client = new ExternalIpClient(mockServer.getUrl());
	}

	@After
	public void stop() {
		if (mockServer != null) {
			mockServer.stop();
		}
	}
}
