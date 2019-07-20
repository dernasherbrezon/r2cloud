package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.List;
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

	@Test
	public void testDeleteDirectory() throws Exception {
		File firstLevel = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		File basedir = new File(firstLevel, UUID.randomUUID().toString());
		assertTrue(basedir.mkdirs());
		try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(basedir, UUID.randomUUID().toString())))) {
			w.append(UUID.randomUUID().toString());
		}
		Util.deleteDirectory(firstLevel.toPath());
		assertFalse(firstLevel.exists());
	}

	@Test
	public void testSplitComma() {
		List<String> result = Util.splitComma("test, , test2");
		assertEquals(2, result.size());
		assertEquals("test", result.get(0));
		assertEquals("test2", result.get(1));
	}

}
