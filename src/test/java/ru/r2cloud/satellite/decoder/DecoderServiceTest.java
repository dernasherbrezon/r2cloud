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
import ru.r2cloud.WxtoimgProcessMock;
import ru.r2cloud.cloud.InfluxDBClient;
import ru.r2cloud.cloud.LeoSatDataService;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.lrpt.PacketReassembly;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.jradio.meteor.MeteorImage;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationDao;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;
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
	public void testApt() throws Exception {
		Observation observation = setupTestData("wxtoimg_noaa18");
		TestUtil.createFile(new File(tempFolder.getRoot(), "channel-a.jpg"), "a");
		TestUtil.createFile(new File(tempFolder.getRoot(), "channel-b.jpg"), "b");
		TestUtil.createFile(new File(tempFolder.getRoot(), "composite.jpg"), "composite");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/wxtoimg_noaa18.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testLrpt() throws Exception {
		Observation observation = setupTestData("lrpt", "source_output.raw.gz");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/lrpt.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testGeoscan() throws Exception {
		Observation observation = setupTestData("geoscan");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/geoscan.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testGaspacs() throws Exception {
		Observation observation = setupTestData("gaspacs");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/gaspacs.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testFox1d() throws Exception {
		Observation observation = setupTestData("fox1d");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/fox1d.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testJy1sat() throws Exception {
		Observation observation = setupTestData("jy1sat");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/jy1sat.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testLucky7() throws Exception {
		Observation observation = setupTestData("lucky7");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/lucky7.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testSharjahsat1() throws Exception {
		Observation observation = setupTestData("sharjahsat1");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/sharjahsat1.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testSputnix() throws Exception {
		Observation observation = setupTestData("sputnix");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/sputnix.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testStratosatTk1() throws Exception {
		Observation observation = setupTestData("stratosattk1");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/stratosattk1.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testRoseyCubesat() throws Exception {
		Observation observation = setupTestData("rosey_cubesat");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/rosey_cubesat.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testEmptyCadu() throws Exception {
		Observation observation = setupTestData("satdump_meteor");
		TestUtil.copyResource(new File(observation.getRawPath().getParentFile(), "meteor.cadu"), "decoderTest/satdump_meteor/empty_cadu.bin");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/satdump_empty_cadu.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testSeries() throws Exception {
		Observation observation = setupTestData("satdump_fengyun3g");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/satdump_fengyun3g.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testResumeFromCadu() throws Exception {
		Observation observation = setupTestData("satdump_meteor");
		TestUtil.copyResource(new File(observation.getRawPath().getParentFile(), "meteor.cadu"), "decoderTest/satdump_meteor/source_cadu.bin");
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
		Observation observation = setupTestData("satdump_meteor");
		TestUtil.copyResource(new File(observation.getRawPath().getParentFile(), "meteor.cadu"), "decoderTest/satdump_meteor/source_cadu.bin");

		processFactory.setDefaultCode(255);
		service.decode(observation.getSatelliteId(), observation.getId());
		assertNull(dao.find(observation.getSatelliteId(), observation.getId()).getInstruments());
	}

	@Test
	public void testNoaa() throws Exception {
		Observation observation = setupTestData("satdump_noaa18");
		service.decode(observation.getSatelliteId(), observation.getId());
		TestUtil.assertJson("expected/satdump_noaa18.json", dao.find(observation.getSatelliteId(), observation.getId()).toJson(null));
	}

	@Test
	public void testInvalidResponse() throws Exception {
		Observation observation = setupTestData("satdump_noaa18");

		processFactory.setDefaultCode(255);
		service.decode(observation.getSatelliteId(), observation.getId());
		assertNull(dao.find(observation.getSatelliteId(), observation.getId()).getInstruments());
	}

	@Test
	public void testDeletedBeforeDecodingStarted() throws Exception {
		Observation observation = setupTestData("aausat4");

		// simulate retention
		assertTrue(observation.getRawPath().delete());

		service.decode(observation.getSatelliteId(), observation.getId());

		observation = dao.find(observation.getSatelliteId(), observation.getId());
		assertEquals(ObservationStatus.FAILED, observation.getStatus());
	}

	@Test
	public void testScheduleTwice() throws Exception {
		Observation observation = setupTestData("aausat4");

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
		String wxtoimg = UUID.randomUUID().toString();
		File basepath = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("r2cloud.newLaunches", false);
		config.setProperty("satellites.basepath.location", basepath.getAbsolutePath());
		config.setProperty("satellites.demod.BPSK", DemodulatorType.FILE.name());
		config.setProperty("satellites.demod.GFSK", DemodulatorType.FILE.name());
		config.remove("r2cloud.apiKey");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-decoder-test.json");
		config.setProperty("satellites.wxtoimg.path", wxtoimg);
		config.update();
		satelliteDao = new SatelliteDao(config);
		dao = new ObservationDao(config);

		HashMap<String, ProcessWrapperMock> reply = new HashMap<>();
		// @formatter:off
		reply.put(wxtoimg, new WxtoimgProcessMock("Satellite: NOAA\n"
				+ "Status: signal processing............................\n"
				+ "Gain: 6.2\n"
				+ "Channel A: 2 (near infrared)\n"
				+ "Channel B: 4 (thermal infrared)"));
		// @formatter:on
		processFactory = new ProcessFactoryMock(reply, UUID.randomUUID().toString());
		decoders = new Decoders(new PredictOreKit(config), config, processFactory, new ExecuteNowThreadFactory(true));
		service = new DecoderService(config, decoders, dao, new LeoSatDataService(config, dao, null, null), new ExecuteNowThreadFactory(true), new InfluxDBClient(config, new DefaultClock()), satelliteDao);
		service.start();
	}

	private Observation setupTestData(String name, String rawFilename) throws Exception {
		File raw = TestUtil.copyResource(tempFolder, "decoderTest/" + name + "/" + rawFilename);
		Observation observation = TestUtil.copyObservation(config.getProperty("satellites.basepath.location"), "decoderTest/" + name);
		dao.insert(observation);
		assertNotNull(dao.update(observation, raw));
		return dao.find(observation.getSatelliteId(), observation.getId());
	}

	private Observation setupTestData(String name) throws Exception {
		return setupTestData(name, "source_output.raw");
	}

}
