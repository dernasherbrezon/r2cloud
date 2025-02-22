package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.FixedClock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.device.RtlSdrDevice;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteSource;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.SdrTransmitterFilter;

public class HousekeepingTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private CelestrakClient celestrak;
	private LeoSatDataClient leosatdata;
	private TleDao tleDao;
	private PriorityService priorityService;
	private Map<String, Tle> tleData;
	private Housekeeping housekeeping;
	private DeviceManager deviceManager;
	private FixedClock clock;

	@Test
	public void testReloadTleForNewSatellites() throws Exception {
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		config.setProperty("r2cloud.newLaunches", true);

		// no new satellites
		String satelliteId = "R2CLOUD3";
		housekeeping.run();
		assertNull(satelliteDao.findById(satelliteId));

		// fast-forward time and setup newly launched satellite
		reset(leosatdata);
		when(leosatdata.loadNewLaunches(anyLong())).thenReturn(SatelliteDao.loadFromClasspathConfig("satellites-leosatdata-newlaunches.json", SatelliteSource.LEOSATDATA));
		clock.setMillis(clock.millis() + 2 * config.getLong("housekeeping.leosatdata.new.periodMillis"));
		housekeeping.run();
		Satellite sat = satelliteDao.findById(satelliteId);
		assertNotNull(sat);
		assertNotNull(sat.getTle());
		assertNotNull(deviceManager.findFirstByTransmitter(sat.getTransmitters().get(0)));

		// this will test if tle and satellite information is saved onto disk
		satelliteDao = new SatelliteDao(config);
		housekeeping = new Housekeeping(config, satelliteDao, null, celestrak, tleDao, null, leosatdata, null, priorityService, deviceManager, clock);
		housekeeping.run();

		sat = satelliteDao.findById(satelliteId);
		assertNotNull(sat);
		assertNotNull(sat.getTle());

		// newly launched satellite was moved to normal
		reset(leosatdata);
		when(leosatdata.loadSatellites(anyLong())).thenReturn(Collections.emptyList());
		clock.setMillis(clock.millis() + 2 * config.getLong("housekeeping.leosatdata.new.periodMillis"));
		housekeeping.run();
		assertNull(satelliteDao.findById(satelliteId));
		assertNull(deviceManager.findFirstByTransmitter(sat.getTransmitters().get(0)));
	}

	@Test
	public void testReloadFailure() {
		String satelliteId = "42784";
		Satellite sat = satelliteDao.findById(satelliteId);
		assertNull(sat.getTle());

		housekeeping.run();
		assertNotNull(sat.getTle());
	}

	@Test
	public void testTleUpdate() throws Exception {
		housekeeping.run();
		Satellite meteor = satelliteDao.findById("42784");
		assertNotNull(meteor);
		assertNotNull(meteor.getTle());
		assertEquals(meteor.getTle().getRaw()[1], "1 42784U 17036V   22114.09726310  .00014064  00000+0  52305-3 0  9992");
		assertEquals(meteor.getTle().getRaw()[2], "2 42784  97.2058 157.8198 0011701 152.8519 207.3335 15.27554982268713");

		clock.setMillis(clock.millis() + 2 * config.getLong("housekeeping.tle.periodMillis"));

		Tle newTle = new Tle(new String[] { "METEOR M-2", "1 40069U 14037A   24217.61569736  .00000303  00000-0  15898-3 0  9993", "2 40069  98.4390 209.6955 0005046 206.9570 153.1346 14.21009510522504" });
		tleData.put("42784", newTle);

		housekeeping.run();

		meteor = satelliteDao.findById("42784");
		assertNotNull(meteor);
		assertNotNull(meteor.getTle());
		assertEquals(newTle.getRaw()[1], meteor.getTle().getRaw()[1]);
		assertEquals(newTle.getRaw()[2], meteor.getTle().getRaw()[2]);
	}

	@Before
	public void start() throws Exception {
		clock = new FixedClock(TestUtil.getTime("2020-09-30 22:17:01.000"));
		tleData = TestUtil.loadTle("sample-tle.txt");

		String rtlsdr = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder, FileSystems.getDefault());
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.json").getAbsolutePath());
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test-schedule.json"); // just smaller number of satellites
		config.setProperty("satellites.leosatdata.location", new File(tempFolder.getRoot(), "leosatdata.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.new.location", new File(tempFolder.getRoot(), "leosatdata.new.json").getAbsolutePath());
		config.setProperty("satellites.rtlsdrwrapper.path", rtlsdr);
		config.setProperty("satellites.satnogs.location", new File(tempFolder.getRoot(), "satnogs.json").getAbsolutePath());
		config.update();

		satelliteDao = new SatelliteDao(config);

		celestrak = mock(CelestrakClient.class);
		when(celestrak.downloadTle()).thenReturn(tleData);
		tleDao = new TleDao(config);

		priorityService = new PriorityService(config, clock);

		leosatdata = mock(LeoSatDataClient.class);
		when(leosatdata.loadSatellites(anyLong())).thenReturn(Collections.emptyList());

		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(rtlsdr, new ProcessWrapperMock(null, null, 0));
		ProcessFactoryMock processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		PredictOreKit predict = new PredictOreKit(config);
		ObservationFactory factory = new ObservationFactory(predict);

		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.OMNIDIRECTIONAL);
		antenna.setMinElevation(8);
		antenna.setGuaranteedElevation(20);

		DeviceConfiguration deviceConfig = new DeviceConfiguration();
		deviceConfig.setId(UUID.randomUUID().toString());
		deviceConfig.setAntennaConfiguration(antenna);
		deviceConfig.setMinimumFrequency(100_000_000);
		deviceConfig.setMaximumFrequency(1_700_000_000);

		RtlSdrDevice device = new RtlSdrDevice(deviceConfig.getId(), new SdrTransmitterFilter(deviceConfig), 1, factory, null, clock, deviceConfig, null, null, predict, null, config, processFactory);

		deviceManager = new DeviceManager(config);
		deviceManager.addDevice(device);

		housekeeping = new Housekeeping(config, satelliteDao, null, celestrak, tleDao, null, leosatdata, null, priorityService, deviceManager, clock);
	}

}
