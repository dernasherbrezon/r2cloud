package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;

public class Ax100DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatelliteDao dao;
	private Decoders decoders;

	@Test
	public void testNoExtraFilesAfterFailure() throws Exception {
		Satellite satellite = dao.findById("44030");
		Transmitter transmitter = satellite.getById("44030-0");
		Decoder decoder = decoders.findByTransmitter(transmitter);
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/delphini1.raw.gz");
		Observation req = TestUtil.loadObservation("data/delphini1.raw.gz.json");
		transmitter.setBandwidth(transmitter.getBandwidth() * 100); // GfskDecoder will fail here
		decoder.decode(wav, req, transmitter);
		File bin = new File(tempFolder.getRoot(), req.getId() + ".bin");
		assertFalse(bin.exists());
	}

	@Before
	public void start() throws Exception {
		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("r2cloud.newLaunches", false);
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.update();
		PredictOreKit predict = new PredictOreKit(config);
		dao = new SatelliteDao(config);
		decoders = new Decoders(predict, config, null);
	}

}
