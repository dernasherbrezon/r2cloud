package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class ObservationSpectrogramTest extends RegisteredTest {

	@Test
	public void testUnknownObservation() {
		HttpResponse<String> response = client.requestObservationSpectogramResponse(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}

	@Test
	public void testInvalidArguments() {
		HttpResponse<String> response = client.requestObservationSpectogramResponse(null, UUID.randomUUID().toString());
		assertEquals(400, response.statusCode());
		assertErrorInField("satelliteId", response);
	}

	@Test
	public void testInvalidArguments2() {
		HttpResponse<String> response = client.requestObservationSpectogramResponse(UUID.randomUUID().toString(), null);
		assertEquals(400, response.statusCode());
		assertErrorInField("id", response);
	}

	@Test
	public void testSuccess() throws Exception {
		String satelliteId = "40069";
		String id = "1560007694942";

		File basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + satelliteId + File.separator + "data" + File.separator + id);
		setupMeta(basepath, "observationSpectrogram/1560007694942.json", id);
		client.getObservation(satelliteId, id);
		// no raw file
		HttpResponse<String> response = client.requestObservationSpectogramResponse(satelliteId, id);
		assertEquals(404, response.statusCode());

		id = "15600076949420";
		basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + satelliteId + File.separator + "data" + File.separator + id);
		setupMeta(basepath, "observationSpectrogram/1560007694942.json", id);
		File rawData = new File(basepath, "output.raw.gz");
		// corrupted gzip file
		try (FileWriter w = new FileWriter(rawData)) {
			w.append(UUID.randomUUID().toString());
		}
		client.getObservation(satelliteId, id);
		response = client.requestObservationSpectogramResponse(satelliteId, id);
		assertEquals(500, response.statusCode());

		id = "15600076949421";
		basepath = new File(config.getProperty("satellites.basepath.location") + File.separator + satelliteId + File.separator + "data" + File.separator + id);
		rawData = new File(basepath, "output.raw.gz");
		setupMeta(basepath, "observationSpectrogram/1560007694942.json", id);
		// correct gzip data
		TestUtil.copy("data/40069-1553411549943.raw.gz", rawData);
		client.getObservation(satelliteId, id);

		JsonObject result = client.requestObservationSpectogram(satelliteId, id);
		String url = result.getString("spectogramURL", null);
		assertNotNull(url);
		HttpResponse<Path> fileResponse = client.downloadFile(url, Paths.get(tempFolder.getRoot().getAbsolutePath(), UUID.randomUUID().toString()));
		assertEquals(200, fileResponse.statusCode());
		TestUtil.assertImage("spectogram-output.raw.gz.png", fileResponse.body().toFile());
	}

	private static void setupMeta(File basepath, String source, String id) throws Exception {
		if (!basepath.exists()) {
			// ignore return code in tests
			basepath.mkdirs();
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ObservationSpectrogramTest.class.getClassLoader().getResourceAsStream(source), StandardCharsets.UTF_8)); BufferedWriter writer = Files.newBufferedWriter(basepath.toPath().resolve("meta.json"))) {
			JsonValue value = Json.parse(reader);
			if (value.isObject()) {
				value.asObject().set("id", id);
			}
			value.writeTo(writer);
		}
	}

}
