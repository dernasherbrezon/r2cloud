package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.model.LRPTResult;
import ru.r2cloud.util.Util;

public class LRPTDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSomeData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = LRPTDecoderTest.class.getClassLoader().getResourceAsStream("lrptSample.wav")) {
			Util.copy(is, fos);
		}
		LRPTResult result = LRPTDecoder.decode(150000, wav);
		assertEquals(6, result.getNumberOfDecodedPackets());
		assertNotNull(result.getData());
		assertNotNull(result.getImage());
	}

	@Test
	public void testNoData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = LRPTDecoderTest.class.getClassLoader().getResourceAsStream("8bit.wav")) {
			Util.copy(is, fos);
		}
		LRPTResult result = LRPTDecoder.decode(150000, wav);
		assertEquals(0, result.getNumberOfDecodedPackets());
		assertNull(result.getData());
		assertNull(result.getImage());
	}

}
