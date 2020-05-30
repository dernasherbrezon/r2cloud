package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.bodies.GeodeticPoint;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Tle;

public class ObservationDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ObservationDao dao;

	@Test
	public void testCrud() throws Exception {
		ObservationRequest req = new ObservationRequest();
		req.setActualFrequency(1L);
		req.setSource(FrequencySource.APT);
		req.setEndTimeMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
		req.setId(UUID.randomUUID().toString());
		req.setInputSampleRate(1);
		req.setTle(create());
		req.setOutputSampleRate(1);
		req.setSatelliteFrequency(1);
		req.setActualFrequency(2);
		req.setSatelliteId(UUID.randomUUID().toString());
		req.setStartTimeMillis(System.currentTimeMillis());
		req.setBandwidth(4_000);
		req.setGroundStation(createGroundStation());
		assertNotNull(dao.insert(req, createTempFile("wav")));
		Observation actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getWavPath());
		assertEquals(req.getSource(), actual.getSource());
		assertNull(actual.getDataPath());
		assertNull(actual.getaPath());
		assertNull(actual.getSpectogramPath());
		assertEquals(1, actual.getSatelliteFrequency());
		assertEquals(2, actual.getActualFrequency());
		assertEquals(4_000, actual.getBandwidth());
		assertEquals(req.getTle(), actual.getTle());
		assertEquals(req.getGroundStation().getLatitude(), actual.getGroundStation().getLatitude(), 0.0);
		assertEquals(req.getGroundStation().getLongitude(), actual.getGroundStation().getLongitude(), 0.0);

		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));

		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("image")));

		assertTrue(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("spectogram")));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getSpectogramPath());

		String gain = UUID.randomUUID().toString();
		Observation full = new Observation(req);
		full.setChannelA(UUID.randomUUID().toString());
		full.setChannelB(UUID.randomUUID().toString());
		full.setGain(gain);
		full.setNumberOfDecodedPackets(1L);
		assertTrue(dao.update(full));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertEquals(gain, actual.getGain());
		assertEquals(1, actual.getNumberOfDecodedPackets().longValue());

		List<Observation> all = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(1, all.size());
	}

	private static Tle create() {
		return new Tle(new String[] { "meteor", "1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990", "2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188" });
	}

	private static GeodeticPoint createGroundStation() {
		GeodeticPoint result = new GeodeticPoint(11.1, -2.333566, 0.0);
		return result;
	}

	private File createTempFile(String data) throws IOException {
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + ".wav");
		try (BufferedWriter w = new BufferedWriter(new FileWriter(result))) {
			w.write(data);
		}
		return result;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		dao = new ObservationDao(config);
	}
}
