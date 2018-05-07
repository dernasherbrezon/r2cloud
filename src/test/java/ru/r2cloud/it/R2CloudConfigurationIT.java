package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredIT;

public class R2CloudConfigurationIT extends RegisteredIT {

	@Test
	public void testSaveAndLoad() {
		String apiKey = UUID.randomUUID().toString();
		boolean syncSpectogram = true;
		client.saveR2CloudConfiguration(apiKey, syncSpectogram);
		JsonObject config = client.getR2CloudConfiguration();
		assertEquals(apiKey, config.getString("apiKey", null));
		assertTrue(config.getBoolean("syncSpectogram", false));
	}

}
