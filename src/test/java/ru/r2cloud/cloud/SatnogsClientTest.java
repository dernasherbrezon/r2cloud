package ru.r2cloud.cloud;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import ru.r2cloud.ClassAdapter;
import ru.r2cloud.DateAdapter;
import ru.r2cloud.FixedClock;
import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.SatnogsServerMock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.model.Satellite;

public class SatnogsClientTest {

	private SatnogsServerMock server;
	private SatnogsClient client;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSuccess() {
		server.setSatellitesMock(new JsonHttpResponse("satnogs/satellites.json", 200));
		server.setTransmittersMock(new JsonHttpResponse("satnogs/transmitters.json", 200));
		server.setTleMock("satnogs");
		List<Satellite> actual = client.loadSatellites();
		Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, new DateAdapter()).registerTypeAdapter(new TypeToken<Class<? extends Beacon>>() {
		}.getType(), new ClassAdapter()).create();
		Type listOfMyClassObject = new TypeToken<List<Satellite>>() {
		}.getType();
		String serialized = GSON.toJson(actual, listOfMyClassObject);
		TestUtil.assertJson("expected/satnogsSatellites.json", Json.parse(serialized).asArray());
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
