package ru.r2cloud.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.FixedClock;
import ru.r2cloud.NoOpTransmitterFilter;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.satellite.SdrTransmitterFilter;

public class DeviceManagerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SatelliteDao satelliteDao;
	private TestConfiguration config;
	private DeviceManager manager;
	private FixedClock clock;
	private DeviceConfiguration deviceConfig;

	@Test
	public void testEnableNotCompatibleSatellite() {
		deviceConfig.setMinimumFrequency(100_000_000);
		deviceConfig.setMaximumFrequency(200_000_000);
		manager.schedule(satelliteDao.findAll());
		assertTrue(manager.findScheduledObservations().isEmpty());
		assertNull(manager.enable(satelliteDao.findById("42784")));
	}

	@Test
	public void testAddTransmitterOnce() {
		deviceConfig.setMinimumFrequency(400_000_000);
		deviceConfig.setMaximumFrequency(500_000_000);
		Satellite sat = satelliteDao.findById("42784");
		manager.schedule(sat);
		List<ObservationRequest> previous = manager.findScheduledObservations();
		manager.schedule(sat);
		List<ObservationRequest> current = manager.findScheduledObservations();
		assertEquals(previous.size(), current.size());
	}

	@Before
	public void start() throws Exception {
		clock = new FixedClock(TestUtil.getTime("2020-09-30 22:17:01.000"));

		String rtlsdr = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(rtlsdr, new ProcessWrapperMock(null, null, 0));
		ProcessFactoryMock processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		config = new TestConfiguration(tempFolder, FileSystems.getDefault());
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test-schedule.json");

		satelliteDao = new SatelliteDao(config);
		satelliteDao.setTle(TestUtil.loadTle("tle-2020-09-27.txt"));

		PredictOreKit predict = new PredictOreKit(config);
		ObservationFactory factory = new ObservationFactory(predict);

		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.OMNIDIRECTIONAL);
		antenna.setMinElevation(8);
		antenna.setGuaranteedElevation(20);

		deviceConfig = new DeviceConfiguration();
		deviceConfig.setId(UUID.randomUUID().toString());
		deviceConfig.setAntennaConfiguration(antenna);

		RtlSdrDevice device = new RtlSdrDevice(deviceConfig.getId(), new SdrTransmitterFilter(deviceConfig, new NoOpTransmitterFilter()), 1, factory, null, clock, deviceConfig, null, null, predict, null, config, processFactory);

		manager = new DeviceManager(null);
		manager.addDevice(device);
	}

}
