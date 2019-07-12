package ru.r2cloud.it;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationListIT extends RegisteredTest {

	@Test
	public void testList() throws IOException {
		copyObservation("1559504588627");
		copyObservation("1560007694942");
		JsonArray satellites = client.getObservationList();
		assertObservation("{\"id\":\"1560007694942\",\"satelliteId\":\"40069\",\"name\":\"METEOR-M 2\",\"start\":1560007694943,\"hasData\":false}", findById(satellites, "1560007694942"));
		assertObservation("{\"id\":\"1559504588627\",\"satelliteId\":\"40069\",\"name\":\"METEOR-M 2\",\"start\":1559504588637,\"hasData\":false}", findById(satellites, "1559504588627"));
	}
	
	private static void assertObservation(String expected, JsonObject actual) {
		TestUtil.assertJson(Json.parse(expected).asObject(), actual);
	}

	private static JsonObject findById(JsonArray array, String id) {
		for (int i = 0; i < array.size(); i++) {
			JsonObject cur = array.get(i).asObject();
			if (cur.getString("id", "").equals(id)) {
				return cur;
			}
		}
		return null;
	}

	private void copyObservation(String name) throws IOException {
		File basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + "40069" + File.separator + "data" + File.separator + name + File.separator + "meta.json");
		TestUtil.copy("observationListIT/" + name + ".json", basepath);
	}

}
