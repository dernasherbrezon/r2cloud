package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.R2CloudServer;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SdrType;

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
		handler.awaitRequestSilently();
		assertNull(handler.getRequest());
	}

	@Test
	public void testLoadNewLaunch() throws Exception {
		server.setNewLaunchMock(new JsonHttpResponse("r2cloudclienttest/newlaunch.json", 200));
		List<Satellite> result = client.loadNewLaunches();
		assertEquals(2, result.size());
		assertSatellite("LUCKY-7", true, result.get(0));
		assertSatellite("PAINANI 1", false, result.get(1));
	}

	@Test
	public void testEmptyNewLaunch() throws Exception {
		assertTrue(client.loadNewLaunches().isEmpty());
	}

	@Test
	public void testInvalidResponseNewLaunches() throws Exception {
		server.setNewLaunchMock("not a json", 200);
		assertTrue(client.loadNewLaunches().isEmpty());
		server.setNewLaunchMock("{ \"test\": 1 }", 200);
		assertTrue(client.loadNewLaunches().isEmpty());
		server.setNewLaunchMock("[ [1,2,3] ]", 200);
		assertTrue(client.loadNewLaunches().isEmpty());

		String validNewLaunchStr = TestUtil.loadExpected("r2cloudclienttest/single-newlaunch.json");
		JsonObject validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("id");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());

		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("name");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());

		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("frequency");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());

		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("modulation");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());

		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.set("modulation", UUID.randomUUID().toString());
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
		
		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("framing");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());

		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.set("framing", UUID.randomUUID().toString());
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
		
		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("bandwidth");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
		
		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("baudRates");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
		
		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.remove("beaconClass");
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
		
		validNewLaunch = Json.parse(validNewLaunchStr).asObject();
		validNewLaunch.set("beaconClass", UUID.randomUUID().toString());
		server.setNewLaunchMock(validNewLaunch);
		assertTrue(client.loadNewLaunches().isEmpty());
	}

	@Before
	public void start() throws Exception {
		server = new R2CloudServer();
		server.start();
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("r2server.hostname", server.getUrl());
		config.setProperty("r2server.connectionTimeout", "1000");
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		config.setProperty("satellites.R2CLOUD3.enabled", false);
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

	private static Observation createRequest() {
		Observation result = new Observation();
		result.setId("1");
		result.setStartTimeMillis(1L);
		result.setEndTimeMillis(1L);
		result.setOutputSampleRate(1);
		result.setInputSampleRate(1);
		result.setSatelliteFrequency(1L);
		result.setActualFrequency(100L);
		result.setSource(FrequencySource.APT);
		result.setSatelliteId("1");
		result.setBandwidth(1000);
		result.setGain("1");
		result.setChannelA("1");
		result.setChannelB("1");
		result.setNumberOfDecodedPackets(1L);
		result.setaURL("1");
		result.setDataURL("1");
		result.setSpectogramURL("1");
		result.setBiast(true);
		result.setSdrType(SdrType.RTLSDR);
		return result;
	}

	private static void assertJson(String filename, String actual) {
		assertEquals(TestUtil.loadExpected(filename), actual);
	}

	private static void assertSatellite(String name, boolean enabled, Satellite actual) {
		assertEquals(name, actual.getName());
		assertEquals(enabled, actual.isEnabled());
		assertNotNull(actual.getTle());
	}
}
