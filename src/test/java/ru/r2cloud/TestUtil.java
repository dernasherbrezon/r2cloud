package ru.r2cloud;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.it.util.WebTest;
import ru.r2cloud.tle.CelestrakClientTest;

public class TestUtil {

	public static String loadExpected(String name) {
		StringBuilder expectedStr = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(CelestrakClientTest.class.getClassLoader().getResourceAsStream(name)))) {
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
		try (BufferedReader r = new BufferedReader(new InputStreamReader(WebTest.class.getClassLoader().getResourceAsStream(classpathFrom), StandardCharsets.UTF_8)); BufferedWriter w = new BufferedWriter(new FileWriter(to))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				w.append(curLine).append("\n");
			}
		}
	}

	public static File setupScript(File to) throws IOException {
		copy(to.getName(), to);
		to.setExecutable(true);
		return to;
	}

	public static void assertJson(JsonObject expected, JsonObject actual) {
		StringBuilder message = new StringBuilder();
		for (String name : expected.names()) {
			JsonValue value = actual.get(name);
			if (value == null) {
				message.append("missing field: " + name).append("\n");
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
