package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.JsonArray;

import ru.r2cloud.FixedClock;
import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.SatnogsServerMock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Satellite;

public class SatnogsClientTest {

	private SatnogsServerMock server;
	private SatnogsClient client;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testInvalidResponse() throws Exception {
		server.setSatellitesMock(new JsonHttpResponse("satnogs/empty-response.json", 404));
		server.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters.json", 200));
		server.setTleMockDirectory("satnogs");
		assertTrue(client.loadSatellites().isEmpty());

		server.setSatellitesMock(new JsonHttpResponse("satnogs/satellites.json", 200));
		server.setTransmittersMock(new JsonHttpResponse("satnogs/empty-response.json", 404));
		server.setTleMockDirectory("satnogs");
		assertTrue(client.loadSatellites().isEmpty());

		server.setSatellitesMock(new JsonHttpResponse("satnogs/satellites-notle.json", 200));
		server.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters-notle.json", 200));
		server.setTleMockDirectory("satnogs");
		List<Satellite> satellite = client.loadSatellites();
		assertEquals(1, satellite.size());
		assertNull(satellite.get(0).getTle());

		server.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters.json", 200));
		server.setTleMockDirectory("satnogs");

		server.setSatellitesMock("not a json", 200);
		assertTrue(client.loadSatellites().isEmpty());
		server.setSatellitesMock("{ \"test\": 1 }", 200);
		assertTrue(client.loadSatellites().isEmpty());
		server.setSatellitesMock("[ [1,2,3] ]", 200);
		assertTrue(client.loadSatellites().isEmpty());

		server.setSatellitesMock(new JsonHttpResponse("satnogs/satellites.json", 200));
		server.setTransmittersMock("not a json", 200);
		assertTrue(client.loadSatellites().isEmpty());
		server.setTransmittersMock("{ \"test\": 1 }", 200);
		assertTrue(client.loadSatellites().isEmpty());
		server.setTransmittersMock("[ [1,2,3] ]", 200);
		assertTrue(client.loadSatellites().isEmpty());
	}

	@Test
	public void testSuccess() {
		server.setSatellitesMock(new JsonHttpResponse("satnogs/satellites.json", 200));
		server.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters.json", 200));
		server.setTleMockDirectory("satnogs");
		List<Satellite> actual = client.loadSatellites();
		JsonArray array = new JsonArray();
		for (Satellite cur : actual) {
			array.add(cur.toJson());
		}
		TestUtil.assertJson("expected/satnogsSatellites.json", array);
	}

	@Before
	public void start() throws Exception {
		server = new SatnogsServerMock();
		server.start();
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("satnogs.hostname", server.getUrl());
		config.setProperty("satnogs.connectionTimeout", "1000");
		client = new SatnogsClient(config, new FixedClock(1660816317365L));
	}

	@After
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}
}
