package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationResult;

public class Aausat4DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	@Ignore
	public void testSomeData() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/aausat4.wav"); 
		Aausat4Decoder decoder = new Aausat4Decoder(config);
		ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("decodertests/Aausat4DecoderTest.json").getReq());
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
