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
		client.saveGeneralConfiguration(config);
		GeneralConfiguration actual = GeneralConfiguration.fromJson(client.getGeneralConfiguration());
		assertEquals(config.getLat(), actual.getLat(), 0.0);
		assertEquals(config.getLng(), actual.getLng(), 0.0);
		assertEquals(config.getAlt(), actual.getAlt(), 0.0);
		assertEquals(config.isAutoUpdate(), actual.isAutoUpdate());
		assertEquals(config.isPresentationMode(), actual.isPresentationMode());
		assertEquals(config.getRetentionRawCount().intValue(), actual.getRetentionRawCount().intValue());
		assertEquals(config.getRetentionMaxSizeBytes().longValue(), actual.getRetentionMaxSizeBytes().longValue());

		JsonObject configured = client.getConfigured();
		assertEquals(config.isPresentationMode(), configured.getBoolean("presentationMode", false));
	}

	@Test
	public void testInvalidLat() {
		GeneralConfiguration config = createConfig();
		config.setLat(null);
		HttpResponse<String> response = client.saveGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("lat", response);
	}

	@Test
	public void testInvalidLng() {
		GeneralConfiguration config = createConfig();
		config.setLng(null);
		HttpResponse<String> response = client.saveGeneralConfigurationWithResponse(config);
		assertEquals(400, response.statusCode());
		assertErrorInField("lng", response);
	}

	private static GeneralConfiguration createConfig() {
		GeneralConfiguration config = new GeneralConfiguration();
		config.setLat(10.1);
		config.setLng(23.4);
		config.setAlt(45.4);
		config.setAutoUpdate(true);
		config.setPresentationMode(true);
		config.setRetentionRawCount(10);
		config.setRetentionMaxSizeBytes(1073741824L);
		return config;
	}
}
