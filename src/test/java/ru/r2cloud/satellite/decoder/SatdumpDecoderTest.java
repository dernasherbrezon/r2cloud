package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.satellite.SatelliteDao;

public class SatdumpDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatdumpDecoder decoder;
	private SatelliteDao satelliteDao;

	@Test
	public void testSuccess() throws Exception {
		DecoderResult result = decoder.decode(new File(UUID.randomUUID().toString()), null, null, null);
		assertNull(result.getRawPath());

		TestUtil.copyFolder(new File("./src/test/resources/satdump_noaa18/").toPath(), tempFolder.getRoot().toPath());
		File raw = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		// create some file
		try (OutputStream os = new FileOutputStream(raw)) {
			os.write(1);
		}

		Observation req = new Observation();
		req.setId(UUID.randomUUID().toString());
		req.setSampleRate(2_400_000);
		Satellite noaa18 = satelliteDao.findById("28654");
		Transmitter lBand = noaa18.getById("28654-0");

		result = decoder.decode(raw, req, lBand, noaa18);
		assertNotNull(result.getInstruments());
		assertEquals(2, result.getInstruments().size()); // test data don't have more than 2 instruments
		Instrument avhrr3 = findById(result, "AVHRR3");
		assertNotNull(avhrr3.getCombinedImagePath());
		assertEquals(6, avhrr3.getChannels().size());
		for (InstrumentChannel cur : avhrr3.getChannels()) {
			assertNotNull(cur.getImagePath());
		}
		Instrument amsu = findById(result, "AMSUA");
		assertNull(amsu.getCombinedImagePath()); // not configured in the test data
		assertEquals(2, amsu.getChannels().size());
		for (InstrumentChannel cur : amsu.getChannels()) {
			assertNotNull(cur.getImagePath());
		}
		// explicitly test disabled instruments
		assertNull(findById(result, "MHS"));
	}

	@Before
	public void start() throws Exception {
		String satdump = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(satdump, new ProcessWrapperMock(null, null, 0));
		ProcessFactoryMock processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		TestConfiguration config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("satellites.satdump.path", satdump);
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-noaa.json");

		satelliteDao = new SatelliteDao(config);
		decoder = new SatdumpDecoder(config, processFactory);
	}

	private static Instrument findById(DecoderResult req, String id) {
		for (Instrument cur : req.getInstruments()) {
			if (cur.getId().equalsIgnoreCase(id)) {
				return cur;
			}
		}
		return null;
	}
}
