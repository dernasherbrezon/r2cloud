package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.ExecuteNowThreadFactory;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.cloud.InfluxDBClient;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.lrpt.PacketReassembly;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.DefaultClock;

public class DecoderServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private DecoderService service;
	private IObservationDao dao;
	private SatelliteDao satelliteDao;
	private Decoders decoders;
	private TestConfiguration config;
	private ProcessFactoryMock processFactory;

	@Test
	public void testEmptyCadu() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_meteor/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_meteor");
		dao.insert(observation);
		raw = dao.update(observation, raw);
		TestUtil.copyResource(new File(raw.getParentFile(), "meteor.cadu"), "satdump_meteor/empty_cadu.bin");

		service.decode(observation.getSatelliteId(), observation.getId());

		TestUtil.assertJson("expected/satdump_empty_cadu.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testSeries() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_fengyun3g/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_fengyun3g");
		dao.insert(observation);
		raw = dao.update(observation, raw);

		service.decode(observation.getSatelliteId(), observation.getId());

		TestUtil.assertJson("expected/satdump_fengyun3g.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testResumeFromCadu() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_meteor/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_meteor");
		dao.insert(observation);
		raw = dao.update(observation, raw);
		TestUtil.copyResource(new File(raw.getParentFile(), "meteor.cadu"), "satdump_meteor/source_cadu.bin");

		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/satdump_meteor.json", observation.toJson(null));
		try (BeaconInputStream<Vcdu> is = new BeaconInputStream<>(new FileInputStream(observation.getDataPath()), Vcdu.class)) {
			MeteorImage meteorImage = new MeteorImage(new PacketReassembly(is));
			TestUtil.assertImage("expected/satdump_meteor.png", meteorImage.toBufferedImage());
		}
	}

	@Test
	public void testCaduInvalidResponse() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_meteor/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_meteor");
		dao.insert(observation);
		raw = dao.update(observation, raw);
		TestUtil.copyResource(new File(raw.getParentFile(), "meteor.cadu"), "satdump_meteor/source_cadu.bin");

		processFactory.setDefaultCode(255);
		service.decode(observation.getSatelliteId(), observation.getId());
		assertNull(dao.find(observation.getSatelliteId(), observation.getId()).getInstruments());
	}

	@Test
	public void testNoaa() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_noaa18/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_noaa18");
		dao.insert(observation);
		assertNotNull(dao.update(observation, raw));

		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/satdump_noaa18.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testInvalidResponse() throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "satdump_noaa18/source_output.raw");
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "satdump_noaa18");
		dao.insert(observation);
		assertNotNull(dao.update(observation, raw));

		processFactory.setDefaultCode(255);
		service.decode(observation.getSatelliteId(), observation.getId());
		assertNull(dao.find(observation.getSatelliteId(), observation.getId()).getInstruments());
	}

	@Test
	public void testDeletedBeforeDecodingStarted() throws Exception {
		File wav = TestUtil.copyResource(tempFolder, "data/aausat.raw.gz");
		Observation observation = TestUtil.loadObservation("data/aausat.raw.gz.json");
		observation.setStatus(ObservationStatus.RECEIVED);
		observation.setNumberOfDecodedPackets(0L);
		dao.insert(observation);
		wav = dao.update(observation, wav);

		// simulate retention
		assertTrue(wav.delete());

		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		assertEquals(ObservationStatus.FAILED, observation.getStatus());
	}

	@Test
	public void testScheduleTwice() throws Exception {
		File wav = TestUtil.copyResource(tempFolder, "data/aausat.raw.gz");
		Observation observation = TestUtil.loadObservation("data/aausat.raw.gz.json");
		observation.setStatus(ObservationStatus.RECEIVED);
		observation.setNumberOfDecodedPackets(0L);
		dao.insert(observation);
		assertNotNull(dao.update(observation, wav));

		service.retryObservations();

		// this will fail observation on the second attempt
		// this shouldn't execute because previous attempt already decoded
		Satellite aausat = satelliteDao.findById(observation.getSatelliteId());
		Transmitter transmitter = aausat.getById(observation.getTransmitterId());
		transmitter.setFraming(Framing.CUSTOM);

		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		assertNotNull(observation);
		assertEquals(1, observation.getNumberOfDecodedPackets().longValue());
	}

	@Before
	public void start() throws Exception {
		File basepath = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("r2cloud.newLaunches", false);
		config.setProperty("satellites.basepath.location", basepath.getAbsolutePath());
		config.remove("r2cloud.apiKey");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.update();
		satelliteDao = new SatelliteDao(config);
		dao = new ObservationDao(config);

		processFactory = new ProcessFactoryMock(new HashMap<>(), UUID.randomUUID().toString());
		decoders = new Decoders(new PredictOreKit(config), config, processFactory);
		service = new DecoderService(config, decoders, dao, new LeoSatDataService(config, dao, null, null), new ExecuteNowThreadFactory(true), new InfluxDBClient(config, new DefaultClock()), satelliteDao);
		service.start();
	}

}
