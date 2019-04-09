package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.it.util.RegisteredTest;

public class OverviewIT extends RegisteredTest {

	@Test
	public void test() {
		JsonObject overview = client.getOverview();
		assertNotNull(overview.get("rtldongle"));
		JsonValue rtltestStatus = overview.get("rtltest");
		assertNotNull(rtltestStatus);
		JsonObject testStatus = rtltestStatus.asObject();
		assertEquals("SUCCESS", testStatus.get("status").asString());
	}
}
