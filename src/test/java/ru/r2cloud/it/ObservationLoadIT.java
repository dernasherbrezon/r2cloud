package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationLoadIT extends RegisteredTest {

	@Test
	public void testLoadAausat4() throws Exception {
		File basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + "41460" + File.separator + "data" + File.separator + "1559942730784");
		TestUtil.copy("aausat4Observation/1559942730784.json", new File(basepath, "meta.json"));
		TestUtil.copy("aausat4Observation/data.bin", new File(basepath, "data.bin"));
		JsonObject observation = client.getObservation("41460", "1559942730784");
		try (InputStreamReader reader = new InputStreamReader(ObservationLoadIT.class.getClassLoader().getResourceAsStream("aausat4Observation/expected.json"))) {
			TestUtil.assertJson(Json.parse(reader).asObject(), observation);
		}
	}

	@Test
	public void testInvalidArguments() {
		HttpResponse<String> response = client.getObservationResponse(null, UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
		assertErrorInField("satelliteId", response);
	}

	@Test
	public void testInvalidArguments2() {
		HttpResponse<String> response = client.getObservationResponse(UUID.randomUUID().toString(), null);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}

	@Test
	public void testUnknownObservation() {
		HttpResponse<String> response = client.getObservationResponse(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}

}
