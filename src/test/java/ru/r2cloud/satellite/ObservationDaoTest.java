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
import ru.r2cloud.model.ObservationRequest;
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
		ObservationRequest req = createRequest();
		assertNotNull(dao.insert(req, createTempFile("wav")));
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
		assertEquals(ObservationStatus.NEW, observation.getStatus());
	}

	@Test
	public void testFindUnknownObservation() throws Exception {
		assertNull(dao.find(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
	}

	@Test
	public void testFindAll() throws Exception {
		int expectedObservations = 5;
		for (int i = 0; i < expectedObservations; i++) {
			ObservationRequest req = createRequest();
			assertNotNull(dao.insert(req, createTempFile("wav")));
		}
		assertEquals(expectedObservations, dao.findAll().size());
	}

	@Test
	public void testRetention() throws Exception {
		String satelliteId = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			ObservationRequest req = createRequest();
			req.setSatelliteId(satelliteId);
			assertNotNull(dao.insert(req, createTempFile("wav")));
		}
		assertEquals(2, dao.findAllBySatelliteId(satelliteId).size());
	}

	@Test
	public void saveSpectogramTwice() throws Exception {
		ObservationRequest req = createRequest();
		assertNotNull(dao.insert(req, createTempFile("wav")));
		assertNotNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void saveImageTwice() throws Exception {
		ObservationRequest req = createRequest();
		assertNotNull(dao.insert(req, createTempFile("wav")));
		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void saveDataTwice() throws Exception {
		ObservationRequest req = createRequest();
		assertNotNull(dao.insert(req, createTempFile("wav")));
		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("dup")));
	}

	@Test
	public void testCrud() throws Exception {
		ObservationRequest req = createRequest();
		assertNotNull(dao.insert(req, createTempFile("wav")));
		Observation actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getRawPath());
		assertNull(actual.getDataPath());
		assertNull(actual.getImagePath());
		assertNull(actual.getSpectogramPath());
		assertEquals(2, actual.getActualFrequency());
		assertEquals(req.getTle(), actual.getTle());
		assertEquals(req.getGroundStation().getLatitude(), actual.getGroundStation().getLatitude(), 0.0);
		assertEquals(req.getGroundStation().getLongitude(), actual.getGroundStation().getLongitude(), 0.0);
		assertEquals(ObservationStatus.NEW, actual.getStatus());

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

		Observation full = new Observation(req);
		full.setChannelA(UUID.randomUUID().toString());
		full.setChannelB(UUID.randomUUID().toString());
		full.setNumberOfDecodedPackets(1L);
		full.setStatus(ObservationStatus.DECODED);
		assertTrue(dao.update(full));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertEquals(45.0, actual.getGain(), 0.0);
		assertEquals(1, actual.getNumberOfDecodedPackets().longValue());
		assertEquals(ObservationStatus.DECODED, actual.getStatus());

		List<Observation> all = dao.findAllBySatelliteId(req.getSatelliteId());
		assertEquals(1, all.size());
	}

	private static ObservationRequest createRequest() {
		ObservationRequest req = new ObservationRequest();
		req.setActualFrequency(1L);
		req.setEndTimeMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
		req.setId(UUID.randomUUID().toString());
		req.setSampleRate(1);
		req.setTle(create());
		req.setActualFrequency(2);
		req.setSatelliteId(UUID.randomUUID().toString());
		req.setTransmitterId(UUID.randomUUID().toString());
		req.setStartTimeMillis(System.currentTimeMillis());
		req.setGroundStation(createGroundStation());
		req.setGain(45.0);
		req.setBiast(false);
		req.setSdrType(SdrType.RTLSDR);
		return req;
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
