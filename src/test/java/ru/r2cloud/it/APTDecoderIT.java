package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.decoder.APTDecoder;
import ru.r2cloud.util.ProcessFactory;

public class APTDecoderIT {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "8bit.wav");
		ObservationRequest request = new ObservationRequest();
		request.setStartLatitude(0.0);
		request.setEndLatitude(0.0);
		APTDecoder decoder = new APTDecoder(config, factory);
		ObservationResult result = decoder.decode(wav, request);
		assertNull(result.getaPath());
		assertEquals("12.0", result.getGain());
		assertEquals("3/3B (mid infrared)", result.getChannelA());
		assertEquals("4 (thermal infrared)", result.getChannelB());
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