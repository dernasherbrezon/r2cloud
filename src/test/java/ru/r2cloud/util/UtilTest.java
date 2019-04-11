package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UtilTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testTotalSamples() throws Exception {
		long totalSamplesExpected = 50;
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (OutputStream fos = new GZIPOutputStream(new FileOutputStream(wav))) {
			for (int i = 0; i < totalSamplesExpected; i++) {
				fos.write(0x01);
			}
		}
		assertEquals(totalSamplesExpected / 2, Util.readTotalSamples(wav).longValue());
	}

	@Test
	public void testUnsignedInt() throws Exception {
		File file = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
		}
		assertEquals((4294967295L / 2), Util.readTotalSamples(file).longValue());
	}

}
