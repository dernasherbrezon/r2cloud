package ru.r2cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.model.DataFormat;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;

public class SpectogramServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSpectogramSuccess() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "8bit.wav");
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
		Observation observation = new Observation();
		observation.setRawPath(new File(UUID.randomUUID().toString()));
		assertNull(service.create(observation));
	}

	@Test
	public void testSpectogramForIq() throws Exception {
		File file = TestUtil.setupClasspathResource(tempFolder, "data/40069-1553411549943.raw.gz");
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

	private static Observation createWav(File wav) {
		Observation result = new Observation(new ObservationRequest());
		result.setRawPath(wav);
		return result;
	}

	private static Observation createIq(File iq, int sampleRate) {
		Observation req = new Observation();
		req.setSampleRate(sampleRate);
		req.setDataFormat(DataFormat.COMPLEX_UNSIGNED_BYTE);
		req.setRawPath(iq);
		return req;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}
}
