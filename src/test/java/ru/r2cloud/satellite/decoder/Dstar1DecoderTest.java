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

public class Dstar1DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/dstar1.raw.gz");
		Dstar1Decoder decoder = new Dstar1Decoder(config);
		ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/dstar1.raw.gz.json").getReq());
		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getIqPath());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
