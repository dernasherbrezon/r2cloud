package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.decoder.MeteorM22Decoder;
import ru.r2cloud.util.ProcessFactory;

public class MeteorM22DecoderIT {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		File file = TestUtil.setupClasspathResource(tempFolder, "data/meteor-m22-small.raw.gz");
		MeteorM22Decoder decoder = new MeteorM22Decoder(config, factory);
		ObservationResult result = decoder.decode(file, TestUtil.loadObservation("decodertests/MeteorM22DecoderTest.json").getReq());
		assertEquals(3L, result.getNumberOfDecodedPackets().longValue());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.meteor_demod.path", "meteor_demod");
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();

		factory = new ProcessFactory();
	}
}
