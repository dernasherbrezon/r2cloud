package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.R2CloudServer;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;

public class R2ServerClientTest {

	private R2CloudServer server;
	private R2ServerClient client;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSaveMeta() {
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/save-meta-response.json", 200);
		server.setObservationMock(handler);
		Long result = client.saveMeta(createRequest());
		assertNotNull(result);
		assertEquals(1L, result.longValue());
		assertEquals("application/json", handler.getRequestContentType());
		assertJson("r2cloudclienttest/save-meta-request.json", handler.getRequest());
	}

	@Test
	public void testAuthFailure() {
		server.setObservationMock(new JsonHttpResponse("r2cloudclienttest/auth-failure-response.json", 403));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testMalformedJsonInResponse() {
		server.setObservationMock(new JsonHttpResponse("r2cloudclienttest/malformed-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testMalformedJsonInResponse2() {
		server.setObservationMock(new JsonHttpResponse("r2cloudclienttest/malformed2-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testInternalFailure() {
		server.setObservationMock(new JsonHttpResponse("r2cloudclienttest/internal-failure-response.json", 200));
		assertNull(client.saveMeta(createRequest()));
	}

	@Test
	public void testInvalidRequest() {
		assertNull(client.saveMeta(null));
	}

	@Test
	public void testSaveMetrics() throws InterruptedException {
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setMetricsMock(handler);
		JsonObject metric = new JsonObject();
		metric.add("name", "temperature");
		metric.add("value", 0.1d);
		JsonArray metrics = new JsonArray();
		metrics.add(metric);
		client.saveMetrics(metrics);
		handler.awaitRequest();
		assertEquals("application/json", handler.getRequestContentType());
		assertJson("r2cloudclienttest/metrics-request.json", handler.getRequest());
	}

	@Test
	public void testSaveBinary() throws Exception {
		long id = 1L;
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setDataMock(id, handler);
		client.saveBinary(id, createFile());
		handler.awaitRequest();
		assertEquals("application/octet-stream", handler.getRequestContentType());
		assertEquals("test", handler.getRequest());
	}

	@Test
	public void testSaveJpeg() throws Exception {
		long id = 1L;
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setDataMock(id, handler);
		client.saveJpeg(id, createFile());
		handler.awaitRequest();
		assertEquals("image/jpeg", handler.getRequestContentType());
		assertEquals("test", handler.getRequest());
	}

	@Test
	public void testSaveSpectogram() throws Exception {
		long id = 1L;
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setSpectogramMock(id, handler);
		client.saveSpectogram(id, createFile());
		handler.awaitRequest();
		assertEquals("image/png", handler.getRequestContentType());
		assertEquals("test", handler.getRequest());
	}

	@Test
	public void testSaveUnknownFile() throws Exception {
		long id = 1L;
		JsonHttpResponse handler = new JsonHttpResponse("r2cloudclienttest/empty-response.json", 200);
		server.setDataMock(id, handler);
		client.saveBinary(id, new File(tempFolder.getRoot(), UUID.randomUUID().toString()));
		handler.awaitRequest();
		assertNull(handler.getRequest());
	}

	@Before
	public void start() throws Exception {
		server = new R2CloudServer();
		server.start();
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("r2server.hostname", server.getUrl());
		config.setProperty("r2server.connectionTimeout", "1000");
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		client = new R2ServerClient(config);
	}

	@After
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	private File createFile() throws IOException {
		File file = new File(tempFolder.getRoot(), "test");
		try (FileWriter fw = new FileWriter(file)) {
			fw.append("test");
		}
		return file;
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
		assertEquals(TestUtil.loadExpected(filename), actual);
	}
}
