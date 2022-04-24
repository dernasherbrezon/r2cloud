package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.bodies.GeodeticPoint;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Tle;

public class ObservationDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ObservationDao dao;
	private MockFileSystem fs;

	@Test
	public void testFailedToUpdate() throws Exception {
		Observation req = createObservation();
		req.setStatus(ObservationStatus.RECEIVED);
		assertNotNull(dao.update(req, createTempFile("wav")));
		Observation observation = dao.find(req.getSatelliteId(), req.getId());
		// corrupt writing
		Path pathToMock = config.getSatellitesBasePath().resolve(req.getSatelliteId()).resolve("data").resolve(req.getId());
		fs.mock(pathToMock, new FailingByteChannelCallback(3));
		observation.setStatus(ObservationStatus.DECODED);
		assertFalse(dao.update(observation));
		fs.removeMock(pathToMock);
		// ensure meta in the state before corruption
		observation = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(observation);
		assertEquals(ObservationStatus.RECEIVED, observation.getStatus());
	}

	@Test
	public void testFindUnknownObservation() throws Exception {
		assertNull(dao.find(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
	}

	@Test
	public void testFindAll() throws Exception {
		int expectedObservations = 5;
		for (int i = 0; i < expectedObservations; i++) {
			assertNotNull(dao.update(createObservation(), createTempFile("wav")));
		}
		assertEquals(expectedObservations, dao.findAll().size());
	}

	@Test
	public void testRetention() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			Observation req = createObservation();
			req.setSatelliteId(satelliteId);
			assertNotNull(dao.update(req, createTempFile("wav")));
		}
		assertEquals(2, dao.findAllBySatelliteId(satelliteId).size());
	}

	@Test
	public void saveSpectogramTwice() throws Exception {
		Observation req = createObservation();
		assertNotNull(dao.update(req, createTempFile("wav")));
		assertNotNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void saveImageTwice() throws Exception {
		Observation req = createObservation();
		assertNotNull(dao.update(req, createTempFile("wav")));
		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void saveDataTwice() throws Exception {
		Observation req = createObservation();
		assertNotNull(dao.update(req, createTempFile("wav")));
		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void testInFlight() throws Exception {
		Observation req = createObservation();
		req.setStatus(ObservationStatus.RECEIVING_DATA);
		dao.insert(req);
		Observation actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual);
		assertEquals(ObservationStatus.RECEIVING_DATA, actual.getStatus());
		List<Observation> observations = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(1, observations.size());

		dao.cancel(req);
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertNull(actual);

		req.setStartTimeMillis(1);
		dao.insert(req);
		Observation req2 = createObservation();
		req2.setSatelliteId(req.getSatelliteId());
		req2.setStartTimeMillis(2);
		assertNotNull(dao.update(req2, createTempFile("wav")));

		List<Observation> all = dao.findAll();
		assertEquals(2, all.size());
		// test desc sorting
		assertEquals(2, all.get(0).getStartTimeMillis());
		assertEquals(1, all.get(1).getStartTimeMillis());

		all = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(2, all.size());
		// test desc sorting
		assertEquals(2, all.get(0).getStartTimeMillis());
		assertEquals(1, all.get(1).getStartTimeMillis());
	}

	@Test
	public void testCrud() throws Exception {
		Observation req = createObservation();
		req.setStatus(ObservationStatus.RECEIVED);
		assertNotNull(dao.update(req, createTempFile("wav")));
		Observation actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getRawPath());
		assertNull(actual.getDataPath());
		assertNull(actual.getImagePath());
		assertNull(actual.getSpectogramPath());
		assertEquals(2, actual.getActualFrequency());
		assertEquals(req.getTle(), actual.getTle());
		assertEquals(req.getGroundStation().getLatitude(), actual.getGroundStation().getLatitude(), 0.0);
		assertEquals(req.getGroundStation().getLongitude(), actual.getGroundStation().getLongitude(), 0.0);
		assertEquals(ObservationStatus.RECEIVED, actual.getStatus());

		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("image")));
		assertNotNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("spectogram")));

		actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getSpectogramPath());
		assertNotNull(actual.getDataPath());
		assertNotNull(actual.getImagePath());
		assertNotNull(actual.getRawPath());
		assertNotNull(actual.getSpectogramURL());
		assertNotNull(actual.getDataURL());
		assertNotNull(actual.getaURL());
		assertNotNull(actual.getRawURL());

		Observation full = new Observation(req.getReq());
		full.setChannelA(UUID.randomUUID().toString());
		full.setChannelB(UUID.randomUUID().toString());
		full.setNumberOfDecodedPackets(1L);
		full.setStatus(ObservationStatus.DECODED);
		assertTrue(dao.update(full));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertEquals("45.0", actual.getGain());
		assertEquals(1, actual.getNumberOfDecodedPackets().longValue());
		assertEquals(ObservationStatus.DECODED, actual.getStatus());

		List<Observation> all = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(1, all.size());
	}

	private static Observation createObservation() {
		Observation result = new Observation();
		result.setActualFrequency(1L);
		result.setEndTimeMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
		result.setId(UUID.randomUUID().toString());
		result.setSampleRate(1);
		result.setTle(create());
		result.setActualFrequency(2);
		result.setSatelliteId(UUID.randomUUID().toString());
		result.setTransmitterId(UUID.randomUUID().toString());
		result.setStartTimeMillis(System.currentTimeMillis());
		result.setGroundStation(createGroundStation());
		result.setGain("45.0");
		result.setBiast(false);
		result.setSdrType(SdrType.RTLSDR);
		return result;
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
		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		dao = new ObservationDao(config);
	}
}
