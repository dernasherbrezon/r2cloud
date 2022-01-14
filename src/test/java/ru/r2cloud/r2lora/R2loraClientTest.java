package ru.r2cloud.r2lora;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.TestUtil;

public class R2loraClientTest {

	private Set<String> configuredContexts = new HashSet<>();

	private HttpServer server;
	private R2loraClient client;

	@Test
	public void testStatus() {
		setupContext("/status", new JsonHttpResponse("r2loratest/status.json", 200));
		R2loraStatus status = client.getStatus();
		assertEquals("IDLE", status.getStatus());
		assertEquals(10, status.getChipTemperature());
		assertEquals(1, status.getConfigs().size());
		ModulationConfig loraConfig = status.getConfigs().get(0);
		assertEquals("lora", loraConfig.getName());
		assertEquals(144.0, loraConfig.getMinFrequency(), 0.0001f);
		assertEquals(500.1, loraConfig.getMaxFrequency(), 0.0001f);
	}

	@Test
	public void testAuthFailure() {
		setupContext("/status", new JsonHttpResponse("r2loratest/status.json", 401));
		R2loraStatus status = client.getStatus();
		assertEquals("CONNECTION_FAILURE", status.getStatus());
	}

	@Test
	public void testStartStop() {
		JsonHttpResponse handler = new JsonHttpResponse("r2loratest/success.json", 200);
		setupContext("/lora/rx/start", handler);
		setupContext("/rx/stop", new JsonHttpResponse("r2loratest/success.json", 200));
		R2loraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		TestUtil.assertJson("r2loratest/request.json", Json.parse(handler.getRequest()).asObject());
		response = client.stopObservation();
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
	}

	@Test
	public void testFailToStart() {
		setupContext("/lora/rx/start", new JsonHttpResponse("r2loratest/failure.json", 200));
		R2loraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("just a failure", response.getFailureMessage());
	}

	@Before
	public void start() throws Exception {
		String host = "localhost";
		int port = 8000;
		server = HttpServer.create(new InetSocketAddress(host, port), 0);
		server.start();
		client = new R2loraClient(host, port, UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10000);
	}

	private void setupContext(String name, HttpHandler handler) {
		if (configuredContexts.remove(name)) {
			server.removeContext(name);
		}
		server.createContext(name, handler);
		configuredContexts.add(name);
	}

	@After
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static R2loraObservationRequest createRequest() {
		R2loraObservationRequest req = new R2loraObservationRequest();
		req.setBw(500.0f);
		req.setCr(7);
		req.setFrequency(433.125f);
		req.setGain(0);
		req.setLdro(0);
		req.setPreambleLength(8);
		req.setSf(9);
		req.setSyncword(18);
		return req;
	}
}
