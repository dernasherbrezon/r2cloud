package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.Predict;
import ru.r2cloud.satellite.decoder.MeteorM22Decoder;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.Util;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class MeteorM22DecoderIT {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		File file = new File(tempFolder.getRoot(), "meteor-m22-small.raw.gz");
		try (FileOutputStream fos = new FileOutputStream(file); InputStream is = MeteorM22DecoderIT.class.getClassLoader().getResourceAsStream("data/meteor-m22-small.raw.gz")) {
			Util.copy(is, fos);
		}
		MeteorM22Decoder decoder = new MeteorM22Decoder(config, new Predict(config), factory);
		ObservationResult result = decoder.decode(file, create());
		assertEquals(2L, result.getNumberOfDecodedPackets().longValue());
	}

	private static ObservationRequest create() {
		// tle at the time of recording
		TLE tle = new TLE(new String[] { "meteor", "1 44387U 19038A   19307.20949296 -.00000024  00000-0  82761-5 0  9994", "2 44387  98.5866 267.2030 0002790  89.8996 270.2502 14.23335259 17202" });
		ObservationRequest result = new ObservationRequest();
		result.setActualFrequency(137900000L);
		result.setSatelliteFrequency(137900000L);
		result.setOrigin(SatelliteFactory.createSatellite(tle));
		result.setStartTimeMillis(1572788081216L);
		result.setInputSampleRate(288000);
		result.setOutputSampleRate(144000);
		result.setBandwidth(140000);
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("locaiton.lat", "37.99");
		config.setProperty("locaiton.lon", "-0.67");
		config.setProperty("satellites.meteor_demod.path", "meteor_demod");
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();

		factory = new ProcessFactory();
	}
}
