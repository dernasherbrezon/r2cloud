package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

import ru.r2cloud.ObservationFullComparator;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.Page;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;
import ru.r2cloud.model.Tle;

public class ObservationDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private IObservationDao dao;
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
			assertNotNull(dao.update(createObservation(String.valueOf(i)), createTempFile("wav")));
		}
		assertEquals(expectedObservations, dao.findAll(new Page()).size());
		List<Observation> actual = dao.findAll(new Page(2));
		assertEquals(2, actual.size());
		assertEquals("4", actual.get(0).getId());
		assertEquals("3", actual.get(1).getId());

		actual = dao.findAll(new Page(null, "3"));
		assertEquals(3, actual.size());
		assertEquals("2", actual.get(0).getId());
		assertEquals("1", actual.get(1).getId());
		assertEquals("0", actual.get(2).getId());

		actual = dao.findAll(new Page(1, "3"));
		assertEquals(1, actual.size());
		assertEquals("2", actual.get(0).getId());
	}

	@Test
	public void testFindAllEdgeCases() throws Exception {
		List<Observation> actual = dao.findAll(new Page());
		assertTrue(actual.isEmpty());

		int expectedObservations = 5;
		for (int i = 0; i < expectedObservations; i++) {
			assertNotNull(dao.update(createObservation(String.valueOf(i)), createTempFile("wav")));
		}

		Page page = new Page();
		page.setSatelliteId(UUID.randomUUID().toString());
		actual = dao.findAll(page);
		assertTrue(actual.isEmpty());

		// past limit
		page = new Page();
		page.setLimit(10);
		actual = dao.findAll(page);
		assertEquals(expectedObservations, actual.size());

		// past last
		page = new Page();
		page.setCursor("0");
		actual = dao.findAll(page);
		assertTrue(actual.isEmpty());

		// trim to null
		page = new Page();
		page.setCursor("");
		actual = dao.findAll(page);
		assertEquals(expectedObservations, actual.size());

		page = new Page();
		page.setSatelliteId("");
		actual = dao.findAll(page);
		assertEquals(expectedObservations, actual.size());

		String satelliteId = UUID.randomUUID().toString();
		String observationId = UUID.randomUUID().toString();
		File observationDir = new File(tempFolder.getRoot().getAbsolutePath(), satelliteId + File.separator + "data" + File.separator + observationId);
		assertTrue(observationDir.mkdirs());
		try (FileOutputStream fos = new FileOutputStream(new File(observationDir, "meta.json"))) {
			// do nothing. create empty file
		}
		assertNull(dao.find(satelliteId, observationId));

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
		Page page = new Page();
		page.setSatelliteId(req.getSatelliteId());
		List<Observation> observations = dao.findAll(page);
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

		List<Observation> all = dao.findAll(new Page());
		assertEquals(2, all.size());
		Collections.sort(all, ObservationFullComparator.INSTANCE);
		// test desc sorting
		assertEquals(2, all.get(0).getStartTimeMillis());
		assertEquals(1, all.get(1).getStartTimeMillis());

		all = dao.findAll(page);
		assertEquals(2, all.size());
		Collections.sort(all, ObservationFullComparator.INSTANCE);
		// test desc sorting
		assertEquals(2, all.get(0).getStartTimeMillis());
		assertEquals(1, all.get(1).getStartTimeMillis());
	}

	@Test
	public void testCrud() throws Exception {
		Observation req = createObservation();
		req.setStatus(ObservationStatus.RECEIVED);
		InstrumentChannel channel0 = new InstrumentChannel();
		channel0.setId("0");
		channel0.setDescription(UUID.randomUUID().toString());
		InstrumentChannel channel1 = new InstrumentChannel();
		channel1.setId("1");
		channel1.setDescription(UUID.randomUUID().toString());
		List<InstrumentChannel> channels = new ArrayList<>();
		channels.add(channel0);
		channels.add(channel1);
		Instrument instrument = new Instrument();
		instrument.setChannels(channels);
		instrument.setId("0");
		instrument.setDescription(UUID.randomUUID().toString());
		instrument.setName(UUID.randomUUID().toString());
		instrument.setSatdumpCombined(UUID.randomUUID().toString());
		instrument.setSatdumpName(UUID.randomUUID().toString());
		Instrument instrument1 = new Instrument();
		instrument1.setId("1");
		instrument1.setDescription(UUID.randomUUID().toString());
		instrument1.setName(UUID.randomUUID().toString());
		instrument1.setSatdumpName(UUID.randomUUID().toString());
		List<Instrument> instruments = new ArrayList<>();
		instruments.add(instrument);
		instruments.add(instrument1);
		req.setInstruments(instruments);

		assertNotNull(dao.update(req, createTempFile("wav")));
		Observation actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getRawPath());
		assertNull(actual.getDataPath());
		assertNull(actual.getImagePath());
		assertNull(actual.getSpectogramPath());
		assertNull(actual.getInstruments().get(0).getCombinedImage());
		assertNull(actual.getInstruments().get(0).getChannels().get(0).getImage());
		assertNull(actual.getInstruments().get(0).getChannels().get(1).getImage());
		assertEquals(2, actual.getFrequency());
		assertEquals(req.getTle(), actual.getTle());
		assertEquals(req.getGroundStation().getLatitude(), actual.getGroundStation().getLatitude(), 0.0);
		assertEquals(req.getGroundStation().getLongitude(), actual.getGroundStation().getLongitude(), 0.0);
		assertEquals(ObservationStatus.RECEIVED, actual.getStatus());
		assertEquals(req.getDevice().getGain(), actual.getDevice().getGain(), 0.0f);
		assertEquals(req.getDevice().getAntennaConfiguration().getGuaranteedElevation(), actual.getDevice().getAntennaConfiguration().getGuaranteedElevation(), 0.0);
		assertEquals(req.getDevice().getRotatorConfiguration().getHostname(), actual.getDevice().getRotatorConfiguration().getHostname());

		assertNotNull(dao.saveData(req.getSatelliteId(), req.getId(), createTempFile("data")));
		assertNotNull(dao.saveImage(req.getSatelliteId(), req.getId(), createTempFile("image")));
		assertNotNull(dao.saveSpectogram(req.getSatelliteId(), req.getId(), createTempFile("spectogram")));
		assertNotNull(dao.saveCombined(req.getSatelliteId(), req.getId(), instrument.getId(), createTempFile("combined")));
		assertNotNull(dao.saveChannel(req.getSatelliteId(), req.getId(), instrument.getId(), channel0.getId(), createTempFile("channel0.png")));
		assertNotNull(dao.saveChannel(req.getSatelliteId(), req.getId(), instrument.getId(), channel1.getId(), createTempFile("channel1.png")));

		actual = dao.find(req.getSatelliteId(), req.getId());
		assertNotNull(actual.getSpectogramPath());
		assertNotNull(actual.getDataPath());
		assertNotNull(actual.getImagePath());
		assertNotNull(actual.getRawPath());
		assertNotNull(actual.getSpectogramURL());
		assertNotNull(actual.getDataURL());
		assertNotNull(actual.getaURL());
		assertNotNull(actual.getRawURL());
		assertNotNull(actual.getInstruments().get(0).getCombinedImageURL());
		assertNotNull(actual.getInstruments().get(0).getChannels().get(0).getImageURL());
		assertNotNull(actual.getInstruments().get(0).getChannels().get(1).getImageURL());
		assertNotNull(actual.getInstruments().get(0).getCombinedImage());
		assertNotNull(actual.getInstruments().get(0).getChannels().get(0).getImage());
		assertNotNull(actual.getInstruments().get(0).getChannels().get(1).getImage());

		Observation full = new Observation(req.getReq());
		full.setChannelA(UUID.randomUUID().toString());
		full.setChannelB(UUID.randomUUID().toString());
		full.setNumberOfDecodedPackets(1L);
		full.setStatus(ObservationStatus.DECODED);
		assertTrue(dao.update(full));
		actual = dao.find(req.getSatelliteId(), req.getId());
		assertEquals(1, actual.getNumberOfDecodedPackets().longValue());
		assertEquals(ObservationStatus.DECODED, actual.getStatus());

		Page page = new Page();
		page.setSatelliteId(req.getSatelliteId());
		List<Observation> all = dao.findAll(page);
		assertEquals(1, all.size());
	}

	private static Observation createObservation() {
		return createObservation(UUID.randomUUID().toString());
	}

	private static Observation createObservation(String id) {
		Observation result = new Observation();
		result.setFrequency(1L);
		result.setEndTimeMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
		result.setId(id);
		result.setSampleRate(1);
		result.setTle(create());
		result.setFrequency(2);
		result.setSatelliteId(UUID.randomUUID().toString());
		result.setTransmitterId(UUID.randomUUID().toString());
		result.setStartTimeMillis(System.currentTimeMillis());
		result.setGroundStation(createGroundStation());
		result.setDevice(createDevice());
		return result;
	}

	private static Tle create() {
		return new Tle(new String[] { "meteor", "1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990", "2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188" });
	}

	private static GeodeticPoint createGroundStation() {
		GeodeticPoint result = new GeodeticPoint(11.1, -2.333566, 0.0);
		return result;
	}

	private static DeviceConfiguration createDevice() {
		DeviceConfiguration result = new DeviceConfiguration();
		result.setBiast(true);
		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.OMNIDIRECTIONAL);
		antenna.setAzimuth(0.0);
		antenna.setBeamwidth(0.0);
		antenna.setElevation(0.0);
		antenna.setGuaranteedElevation(0.0);
		antenna.setMinElevation(5.0);
		antenna.setGuaranteedElevation(15.0);
		result.setAntennaConfiguration(antenna);
		result.setCompencateDcOffset(false);
		result.setGain(2.0f);
		result.setHost("localhost");
		result.setPort(8080);
		result.setId(UUID.randomUUID().toString());
		result.setMaximumBatteryVoltage(4.2);
		result.setMaximumFrequency(1_700_000_000);
		result.setMinimumBatteryVoltage(3.0);
		result.setMinimumFrequency(25_000_000);
		result.setName(UUID.randomUUID().toString());
		result.setPassword(UUID.randomUUID().toString());
		result.setPpm(2);
		RotatorConfiguration rotator = new RotatorConfiguration();
		rotator.setCycleMillis(1000);
		rotator.setHostname("localhost");
		rotator.setId(UUID.randomUUID().toString());
		rotator.setPort(8080);
		rotator.setTimeout(12000);
		rotator.setTolerance(2.0);
		result.setRotatorConfiguration(rotator);
		result.setRtlDeviceId("1");
		SdrServerConfiguration sdrConfig = new SdrServerConfiguration();
		sdrConfig.setBandwidth(1_440_000);
		sdrConfig.setBandwidthCrop(48000);
		sdrConfig.setBasepath("/tmp");
		sdrConfig.setUseGzip(true);
		result.setSdrServerConfiguration(sdrConfig);
		result.setTimeout(24000);
		result.setUsername(UUID.randomUUID().toString());
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

		dao = new ObservationDaoCache(new ObservationDao(config));
	}
}
