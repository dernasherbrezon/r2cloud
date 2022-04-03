package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.AssertJson;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.aausat4.Aausat4Beacon;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.predict.PredictOreKit;

public class Aausat4DecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

//	@Test
//	public void testSomeData() throws Exception {
//		File wav = TestUtil.setupClasspathResource(tempFolder, "data/aausat.raw.gz");
//		PredictOreKit predict = new PredictOreKit(config);
//		Aausat4Decoder decoder = new Aausat4Decoder(predict, config);
//		DecoderResult result = decoder.decode(wav, TestUtil.loadObservation("data/aausat.raw.gz.json").getReq());
//		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
//		assertNotNull(result.getDataPath());
//		try (BeaconInputStream<Aausat4Beacon> is = new BeaconInputStream<>(new FileInputStream(result.getDataPath()), Aausat4Beacon.class)) {
//			assertTrue(is.hasNext());
//			AssertJson.assertObjectsEqual("AAUSAT4Beacon.json", is.next());
//		}
//	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
