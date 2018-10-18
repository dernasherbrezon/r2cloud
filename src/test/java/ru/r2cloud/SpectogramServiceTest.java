package ru.r2cloud;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.util.Util;

public class SpectogramServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSpectogramSuccess() throws Exception {
		File wav = new File(tempFolder.getRoot(), "output.wav");
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = SpectogramServiceTest.class.getClassLoader().getResourceAsStream("8bit.wav")) {
			Util.copy(is, fos);
		}
		SpectogramService service = new SpectogramService(config);
		File result = service.create(wav);
		try (InputStream expected = SpectogramServiceTest.class.getClassLoader().getResourceAsStream("spectogram-output.wav.png"); InputStream actual = new FileInputStream(result)) {
			assertStreamsEqual(expected, actual);
		}
	}

	private static void assertStreamsEqual(InputStream expected, InputStream actual) throws IOException {
		while (true) {
			int expectedByte = expected.read();
			int actualByte = actual.read();
			assertEquals(expectedByte, actualByte);
			if (actualByte == -1 || expectedByte == -1) {
				break;
			}
		}
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}
}
