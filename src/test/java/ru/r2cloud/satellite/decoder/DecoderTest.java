package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;

@RunWith(Parameterized.class)
public class DecoderTest {

	private String rawFile;
	private String rawFileMeta;
	private String satelliteId;
	private String transmitterId;
	private int expectedPackets;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatelliteDao dao;
	private Decoders decoders;

	public DecoderTest(String rawFile, String rawFileMeta, String satelliteId, String transmitterId, int expectedPackets) {
		this.rawFile = rawFile;
		this.rawFileMeta = rawFileMeta;
		this.satelliteId = satelliteId;
		this.transmitterId = transmitterId;
		this.expectedPackets = expectedPackets;
	}

	@Test
	public void testSuccess() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, rawFile);
		Observation req = TestUtil.loadObservation(rawFileMeta);
		Satellite satellite = dao.findById(satelliteId);
		Transmitter transmitter = satellite.getById(transmitterId);
		Decoder decoder = decoders.findByTransmitter(transmitter);
		DecoderResult result = decoder.decode(wav, req, transmitter, satellite);
		assertEquals(expectedPackets, result.getNumberOfDecodedPackets());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getRawPath());
	}

	@Parameterized.Parameters
	public static Collection<?> parameters() {
		return Arrays.asList(new Object[][] { 
			{ "data/aausat.raw.gz", "data/aausat.raw.gz.json", "41460", "41460-0", 1 }, 
			{ "data/chomptt.raw.gz", "data/chomptt.raw.gz.json",  "43855", "43855-0", 1},
			{ "data/delfic3.raw.gz", "data/delfic3.raw.gz.json", "32789", "32789-0", 1},
			{ "data/gomx1.raw.gz", "data/gomx1.raw.gz.json", "39430", "39430-0", 1},
			{ "data/pegasus.raw.gz", "data/pegasus.raw.gz.json", "42784", "42784-0", 1},
			{ "data/tubin.raw.gz", "data/tubin.raw.gz.json", "48900", "48900-0", 1},
			{ "data/rhw.raw.gz", "data/rhw.raw.gz.json", "43743", "43743-0", 2},
			{ "data/itasat1.raw.gz", "data/itasat1.raw.gz.json", "43786", "43786-0", 1},
			{ "data/astrocast.raw.gz", "data/astrocast.raw.gz.json", "43798", "43798-0", 1},
			{ "data/jy1sat.raw.gz", "data/jy1sat.raw.gz.json", "43803", "43803-0", 1},
			{ "data/suomi.raw.gz", "data/suomi.raw.gz.json", "43804", "43804-0", 3},
			{ "data/dstar1.raw.gz", "data/dstar1.raw.gz.json", "43881", "43881-0", 1},
			{ "data/lucky7.raw.gz", "data/lucky7.raw.gz.json", "44406", "44406-0", 1},
			{ "data/opssat.raw.gz", "data/opssat.raw.gz.json", "44878", "44878-0", 1},
			{ "data/fox1c.raw.gz", "data/fox1c.raw.gz.json", "43770", "43770-0", 2},
			{ "data/strand1.raw.gz", "data/strand1.raw.gz.json", "39090", "39090-0", 1},
			{ "data/delfipq.raw.gz", "data/delfipq.raw.gz.json", "51074", "51074-0", 1},
			{ "data/delphini1.raw.gz", "data/delphini1.raw.gz.json", "44030", "44030-0", 2},
			{ "data/meznsat.raw.gz", "data/meznsat.raw.gz.json", "46489", "46489-0", 1},
			{ "data/swampsat2.raw.gz", "data/swampsat2.raw.gz.json", "45115", "45115-0", 1},
			{ "data/40069-1553411549943.raw.gz", "decodertests/LRPTDecoderTest.json", "40069", "40069-0", 5}
		});
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
