package ru.r2cloud.it;

import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.decoder.APTDecoder;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactoryImpl;

public class APTDecoderIT {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "8bit.wav");
		APTDecoder decoder = new APTDecoder(config, factory, new ThreadPoolFactoryImpl(config.getThreadPoolShutdownMillis()));
		DecoderResult result = decoder.decode(wav, new Observation(), new Transmitter(), new Satellite());
		assertNull(result.getImage());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.wxtoimg.path", "wxtoimg");
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();

		factory = new ProcessFactory();
	}

}