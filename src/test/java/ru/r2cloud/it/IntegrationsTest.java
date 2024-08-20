package ru.r2cloud.it;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.model.IntegrationConfiguration;

public class IntegrationsTest extends RegisteredTest {

	@Test
	public void testSaveAndLoad() throws Exception {
		try (InputStreamReader is = new InputStreamReader(IntegrationsTest.class.getClassLoader().getResourceAsStream("integrationsTest/partialConfig.json"), StandardCharsets.UTF_8)) {
			client.saveIntegrationConfiguration(IntegrationConfiguration.fromJson(Json.parse(is).asObject()));
		}
		JsonObject config = client.getIntegrationConfiguration();
		TestUtil.assertJson("integrationsTest/partialConfig.json", config);

		try (InputStreamReader is = new InputStreamReader(IntegrationsTest.class.getClassLoader().getResourceAsStream("integrationsTest/fullConfig.json"), StandardCharsets.UTF_8)) {
			client.saveIntegrationConfiguration(IntegrationConfiguration.fromJson(Json.parse(is).asObject()));
		}
		config = client.getIntegrationConfiguration();
		TestUtil.assertJson("integrationsTest/fullConfig.json", config);
	}

}
