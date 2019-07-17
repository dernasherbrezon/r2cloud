package ru.r2cloud.ddns.noip;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.MockServer;

public class NoIpClientTest {

	private MockServer mockServer;
	private NoIpClient client;

	@Test
	public void testSuccess() throws Exception {
		mockServer.mockResponse("/nic/update", "good 1.2.3.4");
		assertEquals("1.2.3.4", client.update(UUID.randomUUID().toString()));
	}

	@Test(expected = NoIpException.class)
	public void testExpectedError() throws Exception {
		mockServer.mockResponse("/nic/update", "nohost");
		client.update(UUID.randomUUID().toString());
	}

	@Test(expected = NoIpException.class)
	public void testInvalidFormat() throws Exception {
		mockServer.mockResponse("/nic/update", "good1.2.3.4");
		client.update(UUID.randomUUID().toString());
	}
	
	@Test(expected = NoIpException.class)
	public void testUnexpectedError() throws Exception {
		mockServer.mockResponse("/nic/update", UUID.randomUUID().toString());
		client.update(UUID.randomUUID().toString());
	}

	@Test(expected = NoIpException.class)
	public void testNotFound() throws Exception {
		client.update(UUID.randomUUID().toString());
	}

	@Test(expected = RetryException.class)
	public void testRetryException() throws Exception {
		mockServer.mockResponse("/nic/update", "911");
		client.update(UUID.randomUUID().toString());
	}
	
	@Test(expected = NoIpException.class)
	public void testNoconnection() throws Exception {
		mockServer.stop();
		client.update(UUID.randomUUID().toString());
	}

	@Before
	public void start() throws Exception {
		mockServer = new MockServer();
		mockServer.start();
		client = new NoIpClient(mockServer.getUrl(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
	}

	@After
	public void stop() {
		if (mockServer != null) {
			mockServer.stop();
		}
	}
}
