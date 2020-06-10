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
		assertEquals(config.isRotationEnabled(), configuration.getBoolean("rotationEnabled", false));
		assertEquals(config.getRotctrldHostname(), configuration.getString("rotctrldHostname", null));
		assertEquals(config.getRotctrldPort().intValue(), configuration.getInt("rotctrldPort", 0));
		assertEquals(config.getRotatorTolerance(), configuration.getDouble("rotatorTolerance", 0.0), 0.0);
		assertEquals(config.getRotatorCycle().longValue(), configuration.getLong("rotatorCycle", 0));
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
	public void testInvalidElevationMin() {
		GeneralConfiguration config = createConfig();
		config.setElevationMin(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("elevationMin", response);
	}
	
	@Test
	public void testInvalidElevationMin2() {
		GeneralConfiguration config = createConfig();
		config.setElevationMin(-1.0);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("elevationMin", response);
	}
	
	@Test
	public void testInvalidElevationGuaranteed() {
		GeneralConfiguration config = createConfig();
		config.setElevationGuaranteed(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("elevationGuaranteed", response);
	}
	
	@Test
	public void testInvalidElevationGuaranteed2() {
		GeneralConfiguration config = createConfig();
		config.setElevationGuaranteed(91.0);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("elevationGuaranteed", response);
	}
	
	@Test
	public void testInvalidRotctrldHostname() {
		GeneralConfiguration config = createConfig();
		config.setRotctrldHostname(null);
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("rotctrldHostname", response);
	}
	
	@Test
	public void testInvalidRotctrldHostname2() {
		GeneralConfiguration config = createConfig();
		config.setRotctrldHostname(UUID.randomUUID().toString());
		HttpResponse<String> response = client.setGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("rotctrldHostname", response);
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
		config.setRotationEnabled(true);
		config.setRotctrldHostname("127.0.0.1");
		config.setRotctrldPort(4533);
		config.setRotatorTolerance(5.1);
		config.setRotatorCycle(1000L);
		return config;
	}
}
