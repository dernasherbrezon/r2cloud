package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.it.util.RegisteredTest;

public class TleTest extends RegisteredTest {

	@Test
	public void testLoad() {
		JsonObject tle = client.getTle();
		JsonValue data = tle.get("tle");
		assertNotNull(data);
		assertTrue(data.isArray());
		JsonArray dataArray = data.asArray();
		List<String> expected = new ArrayList<>();
		expected.add("METEOR-M 2");
		expected.add("1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990");
		expected.add("2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188");
		assertEquals(expected, findData("40069", dataArray));
	}

	private static List<String> findData(String id, JsonArray array) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < array.size(); i++) {
			JsonObject cur = array.get(i).asObject();
			if (!id.equals(cur.getString("id", null))) {
				continue;
			}
			JsonArray tleData = cur.get("data").asArray();
			for (int j = 0; j < tleData.size(); j++) {
				result.add(tleData.get(j).asString());
			}
		}
		return result;
	}

}
