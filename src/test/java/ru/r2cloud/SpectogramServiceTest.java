package ru.r2cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
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
		File result = service.create(createWav(wav));
		try (InputStream expected = SpectogramServiceTest.class.getClassLoader().getResourceAsStream("spectogram-output.wav.png"); InputStream actual = new FileInputStream(result)) {
			assertStreamsEqual(expected, actual);
		}
	}

	@Test
	public void testNoInputFile() {
		SpectogramService service = new SpectogramService(config);
		assertNull(service.create(null));
	}

	@Test
	public void testSpectogramForIq() throws Exception {
		File file = new File(tempFolder.getRoot(), "output.raw.gz");
		try (FileOutputStream fos = new FileOutputStream(file); InputStream is = SpectogramServiceTest.class.getClassLoader().getResourceAsStream("data/40069-1553411549943.raw.gz")) {
			Util.copy(is, fos);
		}
		SpectogramService service = new SpectogramService(config);
		File result = service.create(createIq(file, 288_000));
		try (InputStream expected = SpectogramServiceTest.class.getClassLoader().getResourceAsStream("spectogram-output.raw.gz.png"); InputStream actual = new FileInputStream(result)) {
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

	private static ObservationFull createWav(File wav) {
		ObservationResult res = new ObservationResult();
		res.setWavPath(wav);
		ObservationFull result = new ObservationFull(new ObservationRequest());
		result.setResult(res);
		return result;
	}

	private static ObservationFull createIq(File iq, int sampleRate) {
		ObservationResult res = new ObservationResult();
		res.setIqPath(iq);
		ObservationRequest req = new ObservationRequest();
		req.setInputSampleRate(sampleRate);
		ObservationFull result = new ObservationFull(req);
		result.setResult(res);
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}
}
