package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.predict.PredictOreKit;

public class Lucky7DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/lucky7.raw.gz");
		PredictOreKit predict = new PredictOreKit(config);
		Lucky7Decoder decoder = new Lucky7Decoder(predict, config);
		ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/lucky7.raw.gz.json").getReq());
		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
