package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.SampleClass;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.SampleRateMapping;

public class UtilTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testLogErrorShortMessage() {
		Logger mock = Mockito.mock(Logger.class);
		Util.logIOException(mock, "unable to save", new CompletionException(createConnectException(new UnresolvedAddressException())));
		Mockito.verify(mock).error("{}: {}", "unable to save", "java.nio.channels.UnresolvedAddressException");
	}

	@Test
	public void testLogErrorShortMessage2() {
		Logger mock = Mockito.mock(Logger.class);
		Util.logIOException(mock, "unable to save", new CompletionException(createConnectException(new IOException("connection refused"))));
		Mockito.verify(mock).error("{}: {}", "unable to save", "connection refused");
	}

	@Test
	public void testRotateImage() throws Exception {
		File rotatedImage = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + ".jpg");
		TestUtil.copy("meteor.spectogram.jpg", rotatedImage);
		Util.rotateImage(rotatedImage);
		TestUtil.assertImage("rotated.meteor.spectogram.jpg", rotatedImage);
	}

	@Test
	public void testTotalSamples() throws Exception {
		long totalSamplesExpected = 50;
		assertEquals(totalSamplesExpected, Util.readTotalBytes(setupGzippedFile(totalSamplesExpected)).longValue());
	}

	@Test
	public void testTotalSamplesForNonGzip() throws Exception {
		byte[] data = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		File file = setupTempFile(data, ".cf32");
		assertEquals(data.length, Util.readTotalBytes(file.toPath()).longValue());
	}

	@Test
	public void testIOException() throws Exception {
		@SuppressWarnings("resource")
		MockFileSystem fs = new MockFileSystem(FileSystems.getDefault());
		Path file = fs.getPath(tempFolder.getRoot().getAbsolutePath()).resolve(UUID.randomUUID().toString() + ".gz");
		long totalSamplesExpected = 50;
		setupGzippedFile(totalSamplesExpected, file);
		fs.mock(file, new FailingByteChannelCallback(3));
		assertNull(Util.readTotalBytes(file));
	}

	@Test
	public void testUnknownFile() {
		assertNull(Util.readTotalBytes(tempFolder.getRoot().toPath().resolve(UUID.randomUUID().toString())));
	}

	@Test
	public void testSmallFile() throws Exception {
		// only 3 bytes
		File file = setupTempFile(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, ".gz");
		assertNull(Util.readTotalBytes(file.toPath()));
	}

	@Test
	public void testUnsignedInt() throws Exception {
		File file = setupTempFile(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, ".gz");
		assertEquals(4294967295L, Util.readTotalBytes(file.toPath()).longValue());
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

	@Test
	public void testSerializeJsonList() {
		assertEquals("{\"f1\":1,\"f10\":\"E2\",\"f11\":[1.1,2.2,3.3],\"f17\":\"010203\",\"f2\":2,\"f3\":3,\"f4\":4,\"f5\":5.1,\"f6\":6.1,\"f7\":\"f7\",\"f8\":[\"1\",\"2\",\"3\"],\"f9\":{\"f9\":[\"1\",\"2\",\"3\"]}}", Util.convertObject(new SampleClass()).toString());
	}

	@Test
	public void testRates() {
		assertEquals(0, Util.convertToReasonableSampleRate(null));
	}

	@Test
	public void testGoodSampleRateRtlSdr() {
		Set<Long> supportedSampleRates = new HashSet<>();
		supportedSampleRates.add(300_000L);
		supportedSampleRates.add(150_000L);
		SampleRateMapping mapping = Util.getSmallestGoodDeviceSampleRate(12_500, supportedSampleRates);
		assertNotNull(mapping);
		assertEquals(300_000L, mapping.getDeviceOutput());
	}

	@Test
	public void testGoodSampleRateSdrServer() {
		Set<Long> supportedSampleRates = new HashSet<>();
		supportedSampleRates.add(120_000L);
		supportedSampleRates.add(48_000L);
		SampleRateMapping mapping = Util.getSmallestGoodDeviceSampleRate(38_400, supportedSampleRates);
		assertNotNull(mapping);
		assertEquals(120_000L, mapping.getDeviceOutput());
	}

	@Test
	public void testMinDividableSampleRate() {
		SampleRateMapping mapping = Util.getSmallestDividableSampleRate(5_000, 240_000L);
		assertNotNull(mapping);
		assertEquals(40_000L, mapping.getDeviceOutput());
	}

	private File setupTempFile(byte[] data, String extension) throws IOException, FileNotFoundException {
		File file = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + extension);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		}
		return file;
	}

	private Path setupGzippedFile(long totalSamplesExpected) throws IOException {
		Path result = tempFolder.getRoot().toPath().resolve(UUID.randomUUID().toString() + ".gz");
		setupGzippedFile(totalSamplesExpected, result);
		return result;
	}

	private static void setupGzippedFile(long totalSamplesExpected, Path result) throws IOException {
		try (OutputStream fos = new GZIPOutputStream(Files.newOutputStream(result))) {
			for (int i = 0; i < totalSamplesExpected; i++) {
				fos.write(0x01);
			}
		}
	}

	private static ConnectException createConnectException(Throwable e) {
		ConnectException result = new ConnectException("connect failed");
		result.initCause(e);
		return result;
	}
}
