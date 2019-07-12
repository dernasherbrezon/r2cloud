package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationListTest extends RegisteredTest {

	@Test
	public void testList() throws IOException {
		copyObservation("1559504588627");
		copyObservation("1560007694942");
		JsonArray satellites = client.getObservationList();
		assertObservation("observationListIT/expected.json", satellites);
	}

	private static void assertObservation(String expected, JsonArray satellites) throws IOException {
		try (InputStreamReader reader = new InputStreamReader(ObservationListTest.class.getClassLoader().getResourceAsStream(expected))) {
			JsonArray expectedArray = Json.parse(reader).asArray();
			assertEquals(expectedArray.size(), satellites.size());
			for (int i = 0; i < expectedArray.size(); i++) {
				TestUtil.assertJson(expectedArray.get(i).asObject(), satellites.get(i).asObject());
			}
		}
	}

	private void copyObservation(String name) throws IOException {
		File basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + "40069" + File.separator + "data" + File.separator + name + File.separator + "meta.json");
		TestUtil.copy("observationListIT/" + name + ".json", basepath);
	}

}
