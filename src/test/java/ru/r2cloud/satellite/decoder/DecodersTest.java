package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;

public class DecodersTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatelliteDao dao;
	private Decoders decoders;
	private TestConfiguration config;

	@Test
	public void testSatelliteHasDecoder() throws Exception {
		for (Satellite cur : dao.findAll()) {
			for (Transmitter curTransmitter : cur.getTransmitters()) {
				assertNotNull("missing decoder for " + curTransmitter.getId(), decoders.findByTransmitter(curTransmitter));
			}
		}
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("satellites.meta.location", "./src/main/resources/satellites.json");
		config.update();
		PredictOreKit predict = new PredictOreKit(config);
		dao = new SatelliteDao(config);
		decoders = new Decoders(predict, config, null);
	}
}
