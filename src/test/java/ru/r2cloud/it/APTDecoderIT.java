package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.APTResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.APTDecoder;
import ru.r2cloud.util.ProcessFactory;

public class APTDecoderIT {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private Satellite satellite;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		config.setProperty("satellites.wxtoimg.path", "wxtoimg");
		File wav = new File(tempFolder.getRoot(), "output.wav");
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = APTDecoderIT.class.getClassLoader().getResourceAsStream("8bit.wav")) {
			copy(is, fos);
		}
		APTDecoder decoder = new APTDecoder(config, factory);
		APTResult result = decoder.decode(wav);
		assertNull(result.getImage());
		assertEquals("12.0", result.getGain());
		assertEquals("3/3B (mid infrared)", result.getChannelA());
		assertEquals("4 (thermal infrared)", result.getChannelB());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.enabled", true);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		satellite = new Satellite();
		satellite.setId(UUID.randomUUID().toString());
		satellite.setFrequency(10);
		satellite.setName(UUID.randomUUID().toString());

		factory = new ProcessFactory();
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024 * 4];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}
}