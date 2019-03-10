package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class ScheduleSaveIT extends RegisteredTest {

	@Test
	public void testUpdateConfiguration() {
		JsonArray schedule = client.getSchedule();
		JsonObject first = (JsonObject) schedule.get(0);
		boolean enabled = first.getBoolean("enabled", false);
		JsonObject result = client.updateSchedule(first.getString("id", null), !enabled);
		if (enabled) {
			assertNull(result.get("nextPass"));
		} else {
			assertNotNull(result.get("nextPass"));
		}
		assertEquals(!enabled, result.getBoolean("enabled", enabled));
	}

}
