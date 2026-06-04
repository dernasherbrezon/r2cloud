package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.orekit.propagation.analytical.tle.TLE;
import org.slf4j.Logger;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.SampleClass;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;

public class UtilTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testTle() throws Exception {
		String[] oldFormat = readTle("tle/oldFormat.txt");

		// initialize default data context for TLE
		TestConfiguration config = new TestConfiguration(tempFolder, FileSystems.getDefault());
		new PredictOreKit(config);

		Tle tle = Util.fromOldFormat(oldFormat);

		// not strictly equals to oldFormat.txt, but within tolerance
		String[] oldFormatCompatible = readTle("tle/oldFormatSerialized.txt");
		TLE predictOld = Util.convert(tle);
		assertEquals(oldFormatCompatible[1], predictOld.getLine1());
		assertEquals(oldFormatCompatible[2], predictOld.getLine2());

		// these 3 not available in the new format
		tle.setLaunchNumber(0);
		tle.setLaunchPiece(null);
		tle.setLaunchYear(0);
		try (Reader is = new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream("tle/newFormat.json"), StandardCharsets.UTF_8)) {
			JsonValue value = Json.parse(is);
			Tle actual = Tle.fromJson(value.asObject());
			TestUtil.assertJson("tle/newFormatSerialized.json", actual.toJson());
			// not available in the old format
			actual.setObjectId(null);
			assertEquals(tle, actual);

			// make compatible with old format
			actual.setLaunchNumber(61);
			actual.setLaunchPiece("G");
			actual.setLaunchYear(2018);
			TLE predictActual = Util.convert(actual);
			assertEquals(oldFormatCompatible[1], predictActual.getLine1());
			assertEquals(oldFormatCompatible[2], predictActual.getLine2());
		}
	}

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
	public void testGoodSampleRateRtlSdr() {
		List<Long> supportedSampleRates = new ArrayList<>();
		supportedSampleRates.add(300_000L);
		supportedSampleRates.add(150_000L);
		assertEquals(300_000L, Util.getSmallestGoodDeviceSampleRate(12_500, supportedSampleRates).longValue());
	}

	@Test
	public void testFractionalSampleRate() {
		List<Long> supportedSampleRates = new ArrayList<>();
		supportedSampleRates.add(300_000L);
		supportedSampleRates.add(150_000L);
		assertEquals(150_000L, Util.getSmallestGoodDeviceSampleRate(13_000, supportedSampleRates).longValue());
	}

	@Test
	public void testGoodSampleRateSdrServer() {
		List<Long> supportedSampleRates = new ArrayList<>();
		supportedSampleRates.add(120_000L);
		supportedSampleRates.add(48_000L);
		assertEquals(120_000L, Util.getSmallestGoodDeviceSampleRate(38_400, supportedSampleRates).longValue());
	}

	@Test
	public void testMinDividableSampleRate() {
		assertEquals(40_000L, Util.getSmallestDividableSampleRate(5_000, 240_000L));
		assertEquals(16_000L, Util.getSmallestDividableSampleRate(5_100, 240_000L));
		assertEquals(21_000L, Util.getSmallestDividableSampleRate(1600, 21_000L));
	}

	@Test
	public void testDemodulatorInput() {
		assertEquals(46_875, Util.getDemodulatorInput(2_400, 46_875));
		assertEquals(15_625, Util.getDemodulatorInput(2_600, 46_875));
		assertEquals(20_000, Util.getDemodulatorInput(1200, 2000000));
	}

	@Test
	public void testSymbolSyncInput() {
		assertEquals(9_375L, Util.getSymbolSyncInput(1250, 46_875));
		assertEquals(21_000L, Util.getSmallestDividableSampleRate(1600, 21_000L));
	}

	@Test
	public void testConvertAzimuthToDegress() {
		assertEquals(90, Util.convertAzimuthToDegress(0), 0.0);
		assertEquals(80, Util.convertAzimuthToDegress(10), 0.0);
		assertEquals(10, Util.convertAzimuthToDegress(80), 0.0);
		assertEquals(0, Util.convertAzimuthToDegress(90), 0.0);
		assertEquals(350, Util.convertAzimuthToDegress(100), 0.0);
		assertEquals(280, Util.convertAzimuthToDegress(170), 0.0);
		assertEquals(270, Util.convertAzimuthToDegress(180), 0.0);
		assertEquals(260, Util.convertAzimuthToDegress(190), 0.0);
		assertEquals(190, Util.convertAzimuthToDegress(260), 0.0);
		assertEquals(180, Util.convertAzimuthToDegress(270), 0.0);
		assertEquals(170, Util.convertAzimuthToDegress(280), 0.0);
		assertEquals(100, Util.convertAzimuthToDegress(350), 0.0);
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

	private static String[] readTle(String resourceName) {
		String[] result = new String[3];
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
			String curLine = null;
			int i = 0;
			while ((curLine = r.readLine()) != null) {
				result[i] = curLine;
				i++;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
