package ru.r2cloud.lora.r2lora;

import static org.junit.Assert.assertArrayEquals;
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
import ru.r2cloud.MultiHttpResponse;
import ru.r2cloud.TestUtil;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;

public class R2loraClientTest {

	private Set<String> configuredContexts = new HashSet<>();

	private HttpServer server;
	private R2loraClient client;

	@Test
	public void testStatus() {
		setupContext("/status", new JsonHttpResponse("r2loratest/status.json", 200));
		LoraStatus status = client.getStatus();
		assertEquals("IDLE", status.getStatus());
		assertEquals(1, status.getConfigs().size());
		ModulationConfig loraConfig = status.getConfigs().get(0);
		assertEquals("lora", loraConfig.getName());
		assertEquals(144.0, loraConfig.getMinFrequency(), 0.0001f);
		assertEquals(500.1, loraConfig.getMaxFrequency(), 0.0001f);
	}

	@Test
	public void testAuthFailure() {
		setupContext("/status", new JsonHttpResponse("r2loratest/authfailure.json", 401));
		LoraStatus status = client.getStatus();
		assertEquals("CONNECTION_FAILURE", status.getStatus());
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		response = client.stopObservation();
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
	}

	@Test
	public void testStartStop() {
		JsonHttpResponse handler = new JsonHttpResponse("r2loratest/success.json", 200);
		setupContext("/lora/rx/start", handler);
		setupContext("/rx/stop", new JsonHttpResponse("r2loratest/successStop.json", 200));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		TestUtil.assertJson("r2loratest/request.json", Json.parse(handler.getRequest()).asObject());
		response = client.stopObservation();
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals(1, response.getFrames().size());
		LoraFrame r2loraFrame = response.getFrames().get(0);
		assertArrayEquals(new byte[] { (byte) 0xca, (byte) 0xfe }, r2loraFrame.getData());
		assertEquals(-121, r2loraFrame.getRssi());
		assertEquals(-5.75, r2loraFrame.getSnr(), 0.00001f);
		assertEquals(-729, r2loraFrame.getFrequencyError());
		assertEquals(1641987504, r2loraFrame.getTimestamp());
	}

	@Test
	public void testFailToStart() {
		setupContext("/lora/rx/start", new JsonHttpResponse("r2loratest/failure.json", 200));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("just a failure", response.getFailureMessage());
	}

	@Test
	public void testStartEvenR2loraIsReceiving() {
		setupContext("/lora/rx/start", new MultiHttpResponse(new JsonHttpResponse("r2loratest/receiving.json", 200), new JsonHttpResponse("r2loratest/success.json", 200)));
		setupContext("/rx/stop", new JsonHttpResponse("r2loratest/successStop.json", 200));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
	}

	@Before
	public void start() throws Exception {
		configuredContexts.clear();
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

	private static LoraObservationRequest createRequest() {
		LoraObservationRequest req = new LoraObservationRequest();
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
