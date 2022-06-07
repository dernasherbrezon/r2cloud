package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.lora.r2lora.R2loraClient;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;

public class R2loraReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Set<String> configuredContexts = new HashSet<>();
	private HttpServer server;
	private R2loraClient client;
	private Configuration config;

	@Test
	public void testStartFailed() throws Exception {
		R2loraReader reader = new R2loraReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
	}

	@Test
	public void testStopFailed() throws Exception {
		JsonHttpResponse handler = new JsonHttpResponse("r2loratest/success.json", 200);
		setupContext("/lora/rx/start", handler);
		R2loraReader reader = new R2loraReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
	}

	@Test
	public void testSuccess() throws Exception {
		JsonHttpResponse handler = new JsonHttpResponse("r2loratest/success.json", 200);
		setupContext("/lora/rx/start", handler);
		setupContext("/rx/stop", new JsonHttpResponse("r2loratest/successStop.json", 200));

		R2loraReader reader = new R2loraReader(config, createValidRequest(), client, createSatellite().getTransmitters().get(0));
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNotNull(iqData);
		assertNotNull(iqData.getDataFile());

		TestUtil.assertJson("r2loratest/request.json", Json.parse(handler.getRequest()).asObject());
	}

	@Before
	public void start() throws Exception {
		configuredContexts.clear();
		String host = "localhost";
		int port = 8000;
		server = HttpServer.create(new InetSocketAddress(host, port), 0);
		server.start();
		client = new R2loraClient(host + ":" + port, UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10000);
		config = new TestConfiguration(tempFolder);
	}

	@After
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static Satellite createSatellite() {
		Transmitter transmitter = new Transmitter();
		transmitter.setLoraCodingRate(7);
		transmitter.setLoraPreambleLength(8);
		transmitter.setLoraSpreadFactor(9);
		transmitter.setLoraSyncword(18);
		transmitter.setLoraLdro(0);
		transmitter.setLoraBandwidth(500000);
		
		Satellite satellite = new Satellite();
		satellite.setId(UUID.randomUUID().toString());
		satellite.setTransmitters(Collections.singletonList(transmitter));
		return satellite;
	}

	private static ObservationRequest createValidRequest() {
		ObservationRequest req = new ObservationRequest();
		req.setActualFrequency(433125000);
		req.setGain(0.0);
		req.setId(UUID.randomUUID().toString());
		return req;
	}

	private void setupContext(String name, HttpHandler handler) {
		if (configuredContexts.remove(name)) {
			server.removeContext(name);
		}
		server.createContext(name, handler);
		configuredContexts.add(name);
	}
}
