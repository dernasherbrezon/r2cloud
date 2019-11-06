package ru.r2cloud;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.UUID;

import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.tle.CelestrakClientTest;
import ru.r2cloud.util.Util;

public class TestUtil {

	public static ObservationFull loadObservation(String classpath) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(classpath), StandardCharsets.UTF_8))) {
			JsonObject meta = Json.parse(r).asObject();
			return ObservationFull.fromJson(meta);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String loadExpected(String name) {
		StringBuilder expectedStr = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(CelestrakClientTest.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8))) {
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

	public static File setupClasspathResource(TemporaryFolder tempFolder, String name) throws IOException {
		URL resource = TestUtil.class.getClassLoader().getResource(name);
		if (resource == null) {
			throw new IllegalArgumentException("unable to find: " + name + " in classpath");
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

	public static File setupScript(File to) throws IOException {
		copy(to.getName(), to);
		to.setExecutable(true);
		return to;
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

	private TestUtil() {
		// do nothing
	}

}
