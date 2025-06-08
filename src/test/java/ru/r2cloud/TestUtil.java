package ru.r2cloud;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.it.ObservationTest;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Util;

public class TestUtil {

	public static Observation loadObservation(String classpath) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(classpath), StandardCharsets.UTF_8))) {
			JsonObject meta = Json.parse(r).asObject();
			return Observation.fromJson(meta);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Observation copyObservation(String basepath, String classpath) throws IOException {
		Observation observation = TestUtil.loadObservation(classpath + "/meta.json");
		File destination = new File(basepath + File.separator + observation.getSatelliteId() + File.separator + "data" + File.separator + observation.getId());
		assertTrue(destination.mkdirs());
		TestUtil.copyFolder(new File("./src/test/resources/" + classpath).toPath(), destination.toPath());
		return observation;
	}

	public static String loadExpected(String name) {
		StringBuilder expectedStr = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				expectedStr.append(curLine).append("\n");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return expectedStr.toString().trim();
	}

	public static String convert(InputStream is) {
		try (StringWriter sw = new StringWriter()) {
			copy(new InputStreamReader(is, StandardCharsets.UTF_8), sw);
			sw.flush();
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File setupClasspathResource(TemporaryFolder tempFolder, String classpathFrom) throws IOException {
		URL resource = TestUtil.class.getClassLoader().getResource(classpathFrom);
		if (resource == null) {
			throw new IllegalArgumentException("unable to find: " + classpathFrom + " in classpath");
		}
		if (resource.getProtocol().equals("file")) {
			return new File(resource.getFile());
		}
		// copy only if resource is in jar
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(result); InputStream is = resource.openStream()) {
			Util.copy(is, fos);
		}
		return result;
	}

	public static File copyClasspathResource(TemporaryFolder tempFolder, String classpathFrom) throws IOException {
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		copy(classpathFrom, result);
		return result;
	}

	public static File copyResource(TemporaryFolder tempFolder, String testResource) throws IOException {
		String extension = "";
		if (testResource.endsWith(".gz")) {
			extension = ".gz";
		}
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + extension);
		File from = new File("src/test/resources/" + testResource);
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(from)); BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(result))) {
			Util.copy(is, os);
		}
		return result;
	}

	public static File copyResource(File to, String testResource) throws IOException {
		File from = new File("src/test/resources/" + testResource);
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(from)); BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(to))) {
			Util.copy(is, os);
		}
		return to;
	}

	public static void copy(Reader input, Writer output) throws IOException {
		char[] buffer = new char[1024 * 4];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	public static void copy(String classpathFrom, File to) throws IOException {
		if (!to.getParentFile().exists() && !to.getParentFile().mkdirs()) {
			throw new IOException("unable to create parent directory: " + to.getParentFile().getAbsolutePath());
		}
		try (InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(classpathFrom); OutputStream w = new FileOutputStream(to)) {
			Util.copy(is, w);
		}
	}

	public static void copyFolder(Path src, Path dest) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static File setupScript(File to) throws IOException {
		copy(to.getName(), to);
		to.setExecutable(true);
		return to;
	}

	public static void assertImage(String expectedFilename, File bais) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(bais))) {
			assertImage(expectedFilename, is);
		}
	}

	public static void assertImage(String expectedFilename, InputStream bais) throws IOException {
		try (InputStream is1 = ObservationTest.class.getClassLoader().getResourceAsStream(expectedFilename)) {
			BufferedImage expected = ImageIO.read(is1);
			BufferedImage actual = ImageIO.read(bais);
			for (int i = 0; i < expected.getWidth(); i++) {
				for (int j = 0; j < expected.getHeight(); j++) {
					assertEquals(expected.getRGB(i, j), actual.getRGB(i, j));
				}
			}
		}
	}

	public static void assertImage(String expectedFilename, BufferedImage actual) throws IOException {
		try (InputStream is1 = ObservationTest.class.getClassLoader().getResourceAsStream(expectedFilename)) {
			BufferedImage expected = ImageIO.read(is1);
			for (int i = 0; i < expected.getWidth(); i++) {
				for (int j = 0; j < expected.getHeight(); j++) {
					assertEquals(expected.getRGB(i, j), actual.getRGB(i, j));
				}
			}
		}
	}

	public static void assertJson(String classPathResource, JsonArray actual) {
		assertNotNull(actual);
		try (Reader is = new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(classPathResource), StandardCharsets.UTF_8)) {
			JsonValue value = Json.parse(is);
			assertTrue(value.isArray());
			JsonArray expected = value.asArray();
			assertEquals(expected.size(), actual.size());
			for (int i = 0; i < expected.size(); i++) {
				assertJson(expected.get(i).asObject(), actual.get(i).asObject());
			}
		} catch (Exception e) {
			fail("unable to assert json: " + classPathResource + " " + e.getMessage());
		}
	}

	public static void assertJson(String classPathResource, JsonObject actual) {
		assertNotNull(actual);
		try (Reader is = new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(classPathResource), StandardCharsets.UTF_8)) {
			JsonValue value = Json.parse(is);
			assertTrue(value.isObject());
			assertJson(value.asObject(), actual);
		} catch (Exception e) {
			fail("unable to assert json: " + classPathResource + " " + e.getMessage());
		}
	}

	public static void assertJson(JsonObject expected, JsonObject actual) {
		StringBuilder message = new StringBuilder();
		for (String name : expected.names()) {
			JsonValue value = actual.get(name);
			if (value == null) {
				message.append("missing field: " + name).append("\n");
				continue;
			}
			if (expected.get(name).isObject() && value.isObject()) {
				assertJson(expected.get(name).asObject(), value.asObject());
				continue;
			}
			if (expected.get(name).isArray() && value.isArray()) {
				JsonArray expectedArray = expected.get(name).asArray();
				JsonArray actualArray = value.asArray();
				assertEquals(expectedArray.size(), actualArray.size());
				for (int i = 0; i < expectedArray.size(); i++) {
					JsonValue expectedValue = expectedArray.get(i);
					JsonValue actualValue = actualArray.get(i);
					if (expectedValue.isObject() && actualValue.isObject()) {
						assertJson(expectedArray.get(i).asObject(), actualArray.get(i).asObject());
					} else {
						if (!actualValue.toString().equals(expectedValue.toString())) {
							message.append("field: \"" + name + "\" expected: " + expectedValue + " actual: " + actualValue + "\n");
						}
					}
				}
				continue;
			}
			String expectedValue = expected.get(name).toString();
			String actualValue = value.toString();
			if (!actualValue.equals(expectedValue)) {
				message.append("field: \"" + name + "\" expected: " + expectedValue + " actual: " + actualValue + "\n");
			}
		}

		if (message.length() > 0) {
			fail(message.toString().trim());
		}
	}

	public static void assertObservation(String classPathResource, JsonObject actual) {
		assertNotNull(actual);
		actual.remove("start");
		actual.remove("end");
		actual.remove("rawURL");
		actual.remove("sigmfDataURL");
		actual.remove("sigmfMetaURL");
		actual.remove("status");
		actual.remove("data");
		TestUtil.assertJson(classPathResource, actual);
	}

	private TestUtil() {
		// do nothing
	}

	public static void assertFile(File actual, File expected) {
		InputStream actualIs = null;
		InputStream expectedIs = null;
		try {
			actualIs = new BufferedInputStream(new FileInputStream(actual));
			if (actual.getName().endsWith("gz")) {
				actualIs = new GZIPInputStream(actualIs);
			}
			expectedIs = new BufferedInputStream(new FileInputStream(expected));
			if (expected.getName().endsWith("gz")) {
				expectedIs = new GZIPInputStream(expectedIs);
			}
			byte[] actualBuf = new byte[1024];
			byte[] expectedBuf = new byte[1024];
			while (!Thread.currentThread().isInterrupted()) {
				int actualBytes = actualIs.read(actualBuf);
				int expectedBytes;
				if (actualBytes == -1) {
					expectedBytes = expectedIs.read(expectedBuf);
				} else {
					expectedBytes = expectedIs.readNBytes(expectedBuf, 0, actualBytes);
				}
				assertEquals(expectedBytes, actualBytes);
				if (actualBytes == -1) {
					break;
				}
				assertArrayEquals(expectedBuf, actualBuf);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			Util.closeQuietly(actualIs);
			Util.closeQuietly(expectedIs);
		}
	}

	public static Map<String, Tle> loadTle(String name, long millis) {
		Map<String, Tle> result = new HashMap<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8))) {
			// only first line matters
			String curLine = null;
			while ((curLine = in.readLine()) != null) {
				String line1 = in.readLine();
				if (line1 == null) {
					break;
				}
				String line2 = in.readLine();
				if (line2 == null) {
					break;
				}
				String noradId = line2.substring(2, 2 + 5).trim();
				Tle value = new Tle(new String[] { curLine.trim(), line1, line2 });
				value.setLastUpdateTime(millis);
				value.setSource("test data");
				result.put(noradId, value);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static Map<String, Tle> loadTle(String name) {
		return loadTle(name, System.currentTimeMillis());
	}

	public static SimpleDateFormat createDateFormatter() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}

	public static long getTime(String str) throws Exception {
		return createDateFormatter().parse(str).getTime();
	}
}
