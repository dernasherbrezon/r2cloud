package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

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

public class Aausat4DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		try (FileOutputStream fos = new FileOutputStream(wav); InputStream is = Aausat4DecoderTest.class.getClassLoader().getResourceAsStream("data/aausat4.wav")) {
			Util.copy(is, fos);
		}
		Aausat4Decoder decoder = new Aausat4Decoder(config, new Predict(config));
		ObservationResult result = decoder.decode(wav, create());
		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
	}

	private static ObservationRequest create() {
		// tle at the time of recording
		TLE tle = new TLE(new String[] { "AAUSAT4", "1 41460U 16025E   18307.40997805  .00001852  00000-0  98049-4 0  9990", "2 41460  98.1158  18.1483 0160661  36.7969 324.4163 15.06405681138443" });
		ObservationRequest result = new ObservationRequest();
		//observation was taken with 8300hz offset
		result.setActualFrequency(437433300L);
		result.setSatelliteFrequency(437425000L);
		result.setOrigin(SatelliteFactory.createSatellite(tle));
		result.setStartTimeMillis(1541360007667L);
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");

		config.setProperty("satellites.enabled", true);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
