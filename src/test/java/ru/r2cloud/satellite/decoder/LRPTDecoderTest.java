package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class LRPTDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = LRPTDecoderTest.class.getClassLoader().getResourceAsStream("data/40069-1553411549943.raw.gz")) {
			Util.copy(is, fos);
		}
		LRPTDecoder decoder = new LRPTDecoder(config, new Predict(config));
		ObservationResult result = decoder.decode(wav, create());
		assertEquals(9, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getaPath());
		assertNotNull(result.getIqPath());
	}

	@Test
	public void testNoData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (OutputStream fos = new GZIPOutputStream(new FileOutputStream(wav))) {
			for (int i = 0; i < 300_000; i++) {
				if (i % 2 == 0) {
					fos.write(0x01);
				} else {
					fos.write(0x00);
				}
			}
		}
		LRPTDecoder decoder = new LRPTDecoder(config, new Predict(config));
		ObservationResult result = decoder.decode(wav, create());
		assertEquals(0, result.getNumberOfDecodedPackets().longValue());
		assertNull(result.getDataPath());
		assertNull(result.getaPath());
		assertNotNull(result.getIqPath());
	}

	private static ObservationRequest create() {
		// tle at the time of recording
		TLE tle = new TLE(new String[] { "meteor", "1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990", "2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188" });
		ObservationRequest result = new ObservationRequest();
		result.setActualFrequency(137898800L);
		result.setSatelliteFrequency(137900000L);
		result.setOrigin(SatelliteFactory.createSatellite(tle));
		result.setStartTimeMillis(1554483451541L);
		result.setInputSampleRate(300_000);
		result.setOutputSampleRate(150_000);
		result.setBandwidth(140000);
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "53.72");
		config.setProperty("locaiton.lon", "47.57");

		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}
}
