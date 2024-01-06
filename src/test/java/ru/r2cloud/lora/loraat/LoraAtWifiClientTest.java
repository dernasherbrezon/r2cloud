package ru.r2cloud.lora.loraat;

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
import ru.r2cloud.TestUtil;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;

public class LoraAtWifiClientTest {

	private Set<String> configuredContexts = new HashSet<>();

	private HttpServer server;
	private LoraAtWifiClient client;

	@Test
	public void testStatus() {
		setupContext("/api/v2/status", new JsonHttpResponse("loraatwifitest/status.json", 200));
		LoraStatus status = client.getStatus();
		assertEquals("IDLE", status.getStatus());
		assertEquals(1, status.getConfigs().size());
		ModulationConfig loraConfig = status.getConfigs().get(0);
		assertEquals("lora", loraConfig.getName());
		assertEquals(144000000, loraConfig.getMinFrequency());
		assertEquals(500100000, loraConfig.getMaxFrequency());
	}

	@Test
	public void testAuthFailure() {
		setupContext("/api/v2/status", new JsonHttpResponse("loraatwifitest/authfailure.json", 401));
		LoraStatus status = client.getStatus();
		assertEquals("CONNECTION_FAILURE", status.getStatus());
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		response = client.stopObservation();
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
	}

	@Test
	public void testStartStop() {
		JsonHttpResponse handler = new JsonHttpResponse("loraatwifitest/success.json", 200);
		setupContext("/api/v2/lora/rx/start", handler);
		setupContext("/api/v2/rx/stop", new JsonHttpResponse("loraatwifitest/successStop.json", 200));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		TestUtil.assertJson("loraatwifitest/request.json", Json.parse(handler.getRequest()).asObject());
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
		setupContext("/api/v2/lora/rx/start", new JsonHttpResponse("loraatwifitest/failure.json", 200));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("just a failure", response.getFailureMessage());
	}

	@Before
	public void start() throws Exception {
		configuredContexts.clear();
		String host = "localhost";
		int port = 8000;
		server = HttpServer.create(new InetSocketAddress(host, port), 0);
		server.start();
		client = new LoraAtWifiClient(host, port, UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10000);
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
		req.setBw(500000);
		req.setCr(7);
		req.setFrequency(433125000);
		req.setGain(0);
		req.setLdro(0);
		req.setPreambleLength(8);
		req.setSf(9);
		req.setSyncword(18);
		req.setUseCrc(true);
		req.setUseExplicitHeader(true);
		req.setBeaconSizeBytes(255);
		return req;
	}
}
