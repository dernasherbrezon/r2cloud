package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.predict.PredictOreKit;

public class LRPTDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/40069-1553411549943.raw.gz");
		PredictOreKit predict = new PredictOreKit(config);
		LRPTDecoder decoder = new LRPTDecoder(predict, config);
		DecoderResult result = decoder.decode(wav, TestUtil.loadObservation("decodertests/LRPTDecoderTest.json").getReq());
		assertEquals(6, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getImagePath());
		assertNotNull(result.getRawPath());
	}

	@Test
	public void testNoData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (OutputStream fos = new GZIPOutputStream(new FileOutputStream(wav))) {
			for (int i = 0; i < 300_000; i++) {
				if (i % 2 == 0) {
					fos.write(0x01);
				} else {
					fos.write(0x00);
				}
			}
		}
		PredictOreKit predict = new PredictOreKit(config);
		LRPTDecoder decoder = new LRPTDecoder(predict, config);
		DecoderResult result = decoder.decode(wav, TestUtil.loadObservation("decodertests/LRPTDecoderTest.json").getReq());
		assertEquals(0, result.getNumberOfDecodedPackets().longValue());
		assertNull(result.getDataPath());
		assertNull(result.getImagePath());
		assertNotNull(result.getRawPath());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}
}
