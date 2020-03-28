package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationResult;

public class SmogPDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testTelemetry() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/smogp.gz");
		SmogPDecoder decoder = new SmogPDecoder(config);
		ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/smogp.raw.gz.json").getReq());
		assertEquals(2, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getIqPath());
	}

	@Test
//	@Ignore
	public void testSomeData2() throws Exception {
//		File wav = TestUtil.setupClasspathResource(tempFolder, "data/suomi.raw.gz");
//		 File wav = new File("/Users/dernasherbrezon/Downloads/1584939126013/output.raw.gz");
//		 File meta = new File("/Users/dernasherbrezon/Downloads/1584939126013/meta.json");
//		File wav = new File("/Users/dernasherbrezon/Downloads/1585285781613/output.raw.gz");
//		File meta = new File("/Users/dernasherbrezon/Downloads/1585285781613/meta.json");
//		 File wav = new File("/Users/dernasherbrezon/Downloads/1584939126013/output.raw.gz-136-139.raw.gz");
//		 File meta = new File("/Users/dernasherbrezon/Downloads/1584939126013/output.raw.gz-136-139.raw.gz.json");
		 File wav = new File("/Users/dernasherbrezon/Downloads/1585285781613/output.raw.gz-206-210.raw.gz");
		 File meta = new File("/Users/dernasherbrezon/Downloads/1585285781613/output.raw.gz-206-210.raw.gz.json");
		ObservationFull observation = load(meta);
		
		SmogPDecoder decoder = new SmogPDecoder(config);
//		ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/suomi.raw.gz.json").getReq());
		ObservationResult result = decoder.decode(wav, observation.getReq());
		assertEquals(2, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getIqPath());
	}

	private static ObservationFull load(File file) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			JsonObject meta = Json.parse(r).asObject();
			return ObservationFull.fromJson(meta);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
