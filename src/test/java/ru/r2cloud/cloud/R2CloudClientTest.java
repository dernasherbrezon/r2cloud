package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.Util;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;

public class R2CloudClientTest {

	private HttpServer server;
	private R2CloudClient client;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSaveMeta() {
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.createContext("/api/v1/observation", handler);
		Long result = client.saveMeta(createRequest());
		assertNotNull(result);
		assertEquals(1L, result.longValue());
		assertEquals("application/json", handler.getRequestContentType());
		assertJson("r2cloudclienttest/save-meta-request.json", handler.getRequest());
	}

	@Test
	public void testAuthFailure() {
		server.createContext("/api/v1/observation", new JsonHttpResponse("r2cloudclienttest/auth-failure-response.json", 403));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testMalformedJsonInResponse() {
		server.createContext("/api/v1/observation", new JsonHttpResponse("r2cloudclienttest/malformed-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testMalformedJsonInResponse2() {
		server.createContext("/api/v1/observation", new JsonHttpResponse("r2cloudclienttest/malformed2-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}
	
	@Test
	public void testInternalFailure() {
		server.createContext("/api/v1/observation", new JsonHttpResponse("r2cloudclienttest/internal-failure-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}
	
	@Before
	public void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
		server.start();
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("r2cloud.hostname", "http://localhost:8001");
		config.setProperty("r2cloud.connectionTimeout", "1000");
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		client = new R2CloudClient(config);
	}

	@After
	public void stop() throws Exception {
		server.stop(0);
	}

	private static ObservationFull createRequest() {
		ObservationRequest req = new ObservationRequest();
		req.setId("1");
		req.setStartTimeMillis(1L);
		req.setEndTimeMillis(1L);
		req.setOutputSampleRate(1);
		req.setInputSampleRate(1);
		req.setSatelliteFrequency(1L);
		req.setActualFrequency(100L);
		req.setSource(FrequencySource.APT);
		req.setSatelliteId("1");
		ObservationResult res = new ObservationResult();
		res.setGain("1");
		res.setChannelA("1");
		res.setChannelB("1");
		res.setNumberOfDecodedPackets(1L);
		res.setaURL("1");
		res.setDataURL("1");
		res.setSpectogramURL("1");
		ObservationFull observation = new ObservationFull(req);
		observation.setResult(res);
		return observation;
	}

	private static void assertJson(String filename, String actual) {
		assertEquals(Util.loadExpected(filename), actual);
	}
}
