package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;

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
		assertEquals(config.isPresentationMode(), configuration.getBoolean("presentationMode", false));

		JsonObject configured = client.getConfigured();
		assertEquals(config.isPresentationMode(), configured.getBoolean("presentationMode", false));
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

	private static GeneralConfiguration createConfig() {
		GeneralConfiguration config = new GeneralConfiguration();
		config.setLat(10.1);
		config.setLng(23.4);
		config.setAutoUpdate(true);
		config.setPresentationMode(true);
		return config;
	}
}
