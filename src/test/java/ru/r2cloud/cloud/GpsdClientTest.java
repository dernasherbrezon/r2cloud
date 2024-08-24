package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.GpsdMock;
import ru.r2cloud.TestConfiguration;

public class GpsdClientTest {

	private GpsdMock server;
	private GpsdClient client;
	private TestConfiguration config;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSuccess() {
		client.updateCoordinates();
		assertCoordinates(null, null);

		config.setProperty("location.auto", true);
		config.setProperty("location.gpsd.hostname", "255.255.255.255");
		client.updateCoordinates();
		assertCoordinates(null, null);

		config.setProperty("location.gpsd.hostname", server.getHostname());
		client.updateCoordinates();
		assertCoordinates(null, null);

		List<JsonObject> response = new ArrayList<>();
		JsonObject version = createType("VERSION");
		version.set("release", "3.22");
		response.add(version);
		response.add(new JsonObject());
		response.add(createType("SKY"));
		JsonObject tpvNoFix = createType("TPV");
		tpvNoFix.set("mode", 0);
		response.add(tpvNoFix);
		JsonObject tpv = createType("TPV");
		tpv.set("mode", 2);
		tpv.set("lat", 0.123);
		tpv.set("lon", 53.22);
		response.add(tpv);
		server.setResponse(response);
		config.setProperty("location.gpsd.fixTimeout", 60000);
		client.updateCoordinates();
		assertCoordinates(0.123, 53.22);
	}

	private static JsonObject createType(String clazz) {
		JsonObject result = new JsonObject();
		result.set("class", clazz);
		return result;
	}

	private void assertCoordinates(Double lat, Double lon) {
		Double actualLat = config.getDouble("locaiton.lat");
		if (lat == null) {
			assertNull(actualLat);
		} else {
			assertEquals(lat, actualLat, 0.00001f);
		}
		Double actualLon = config.getDouble("locaiton.lon");
		if (lon == null) {
			assertNull(actualLon);
		} else {
			assertEquals(lon, actualLon, 0.00001f);
		}
	}

	@Before
	public void start() throws Exception {
		server = new GpsdMock();
		server.start();
		config = new TestConfiguration(tempFolder);
		config.setProperty("location.auto", false);
		config.setProperty("location.gpsd.hostname", server.getHostname());
		config.setProperty("location.gpsd.port", server.getPort());
		config.remove("locaiton.lat");
		config.remove("locaiton.lon");
		client = new GpsdClient(config);
	}

	@After
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}
}
