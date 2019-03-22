package ru.r2cloud.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleSaveIT extends RegisteredTest {

	@Test
	public void testUpdateConfiguration() {
		String satelliteId = "40069";
		JsonObject result = client.updateSchedule(satelliteId, true);
		assertNotNull(result.get("nextPass"));
		assertTrue(result.getBoolean("enabled", false));
		result = client.updateSchedule(satelliteId, false);
		assertNull(result.get("nextPass"));
	}

}
