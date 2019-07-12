package ru.r2cloud.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.BaseTest;

public class ConfiguredTest extends BaseTest {

	@Test
	public void testGetConfigured() {
		client.resetPassword(username);
		JsonObject configured = client.getConfigured();
		assertFalse(configured.getBoolean("configured", true));
	}

	@Test
	public void testGetConfiguredSuccess() {
		client.setup(keyword, username, password);
		JsonObject configured = client.getConfigured();
		assertTrue(configured.getBoolean("configured", false));
	}

}
