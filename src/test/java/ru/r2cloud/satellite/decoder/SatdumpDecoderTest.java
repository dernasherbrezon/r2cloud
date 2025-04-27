package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.lrpt.LRPTInputStream;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.model.DataFormat;
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
	private TestConfiguration config;

	@Test
	public void testMeteor() throws Exception {
		TestUtil.copyFolder(new File("./src/test/resources/satdump_meteor/").toPath(), tempFolder.getRoot().toPath());
		File raw = new File(tempFolder.getRoot(), "output.raw");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-meteor.json");
		satelliteDao = new SatelliteDao(config);
		
		Observation req = new Observation();
		req.setId(UUID.randomUUID().toString());
		req.setSampleRate(288_000);
		req.setDataFormat(DataFormat.COMPLEX_UNSIGNED_BYTE);
		Satellite meteormn23 = satelliteDao.findById("57166");
		Transmitter lrpt = meteormn23.getById("57166-0");

		DecoderResult result = decoder.decode(raw, req, lrpt, meteormn23);
		assertNotNull(result.getInstruments());
		assertEquals(1, result.getInstruments().size());
		assertNotNull(result.getData());
		List<Vcdu> data = new ArrayList<>();
		try (LRPTInputStream is = new LRPTInputStream(new BufferedInputStream(new FileInputStream(result.getData())))) {
			while (is.hasNext()) {
				data.add(is.next());
			}
		}
		assertEquals(2, data.size());
	}

	@Test
	public void testSuccess() throws Exception {
		DecoderResult result = decoder.decode(new File(UUID.randomUUID().toString()), null, null, null);
		assertNull(result.getIq());

		TestUtil.copyFolder(new File("./src/test/resources/satdump_noaa18/").toPath(), tempFolder.getRoot().toPath());
		File raw = new File(tempFolder.getRoot(), "output.raw");

		Observation req = new Observation();
		req.setId(UUID.randomUUID().toString());
		req.setSampleRate(2_400_000);
		req.setDataFormat(DataFormat.COMPLEX_UNSIGNED_BYTE);
		Satellite noaa18 = satelliteDao.findById("28654");
		Transmitter lBand = noaa18.getById("28654-0");

		result = decoder.decode(raw, req, lBand, noaa18);
		assertNotNull(result.getInstruments());
		assertEquals(3, result.getInstruments().size()); // test data don't have more than 3 instruments
		Instrument avhrr3 = findById(result, "AVHRR3");
		assertNotNull(avhrr3.getCombinedImage());
		assertEquals(6, avhrr3.getChannels().size());
		for (InstrumentChannel cur : avhrr3.getChannels()) {
			assertNotNull(cur.getImage());
		}
		Instrument amsu = findById(result, "AMSUA");
		assertNull(amsu.getCombinedImage()); // not configured in the test data
		assertEquals(2, amsu.getChannels().size());
		for (InstrumentChannel cur : amsu.getChannels()) {
			assertNotNull(cur.getImage());
		}
		// explicitly test disabled instruments
		assertNull(findById(result, "MHS"));
		Instrument sem = findById(result, "SEM2");
		assertNotNull(sem.getCombinedImage());
		assertNull(sem.getChannels());
	}

	@Before
	public void start() throws Exception {
		String satdump = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(satdump, new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false));
		ProcessFactoryMock processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		config = new TestConfiguration(tempFolder);
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
