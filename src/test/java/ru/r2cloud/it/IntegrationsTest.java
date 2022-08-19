package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class IntegrationsTest extends RegisteredTest {

	@Test
	public void testSaveAndLoad() {
		String apiKey = UUID.randomUUID().toString();
		client.saveR2CloudConfiguration(apiKey, true, true, true);
		JsonObject config = client.getR2CloudConfiguration();
		assertEquals(apiKey, config.getString("apiKey", null));
		assertTrue(config.getBoolean("syncSpectogram", false));
		assertTrue(config.getBoolean("newLaunch", false));
		assertTrue(config.getBoolean("satnogs", false));
	}

}
