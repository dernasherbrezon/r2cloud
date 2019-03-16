package ru.r2cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import ru.r2cloud.tle.CelestrakClientTest;

public class Util {

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

	private Util() {
		// do nothing
	}

}
