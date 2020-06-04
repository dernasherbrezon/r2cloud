package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.model.GeneralConfiguration;

public class GeneralConfigurationTest extends RegisteredTest {

	@Test
	public void testConfiguration() {
		GeneralConfiguration config = createConfig();
		client.setGeneralConfiguration(config);
		JsonObject configuration = client.getGeneralConfiguration();
		assertEquals(config.getLat(), configuration.getDouble("lat", 0.0), 0.0);
		assertEquals(config.getLng(), configuration.getDouble("lng", 0.0), 0.0);
		assertEquals(config.isAutoUpdate(), configuration.getBoolean("autoUpdate", !config.isAutoUpdate()));
		assertEquals(config.getPpmType(), configuration.getString("ppmType", null));
		assertEquals(config.getPpm().intValue(), configuration.getInt("ppm", -1));
		assertEquals(config.getElevationMin(), configuration.getDouble("elevationMin", 0.0), 0.0);
		assertEquals(config.getElevationGuaranteed(), configuration.getDouble("elevationGuaranteed", 0.0), 0.0);
	}

	@Test
	public void testDefaultPpmType() {
		GeneralConfiguration config = createConfig();
		config.setPpmType(null);
		client.setGeneralConfiguration(config);
		JsonObject configuration = client.getGeneralConfiguration();
		assertEquals(config.getLat(), configuration.getDouble("lat", 0.0), 0.0);
		assertEquals(config.getLng(), configuration.getDouble("lng", 0.0), 0.0);
		assertEquals(config.isAutoUpdate(), configuration.getBoolean("autoUpdate", !config.isAutoUpdate()));
		assertEquals("AUTO", configuration.getString("ppmType", null));
		assertEquals(config.getElevationMin(), configuration.getDouble("elevationMin", 0.0), 0.0);
		assertEquals(config.getElevationGuaranteed(), configuration.getDouble("elevationGuaranteed", 0.0), 0.0);
	}

	@Test
	public void testInvalidLat() {
		GeneralConfiguration config = createConfig();
		config.setLat(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("lat", response);
	}

	@Test
	public void testInvalidLng() {
		GeneralConfiguration config = createConfig();
		config.setLng(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("lng", response);
	}

	@Test
	public void testMissingPpm() {
		GeneralConfiguration config = createConfig();
		config.setPpmType("MANUAL");
		config.setPpm(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("ppm", response);
	}

	@Test
	public void testUnknownPpmType() {
		GeneralConfiguration config = createConfig();
		config.setPpmType(UUID.randomUUID().toString());
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("ppmType", response);
	}

	private static GeneralConfiguration createConfig() {
		GeneralConfiguration config = new GeneralConfiguration();
		config.setLat(10.1);
		config.setLng(23.4);
		config.setAutoUpdate(true);
		config.setPpmType("MANUAL");
		config.setPpm(10);
		config.setElevationMin(8d);
		config.setElevationGuaranteed(20d);
		return config;
	}
}
