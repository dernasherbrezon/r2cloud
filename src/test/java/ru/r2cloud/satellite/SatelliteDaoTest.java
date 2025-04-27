package ru.r2cloud.satellite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.FixedClock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteSource;

public class SatelliteDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private FixedClock clock;

	@Test
	public void testLocalConfigOverrideRemote() {
		satelliteDao.saveLeosatdata(SatelliteDao.loadFromClasspathConfig("satellites-leosatdata-older.json", SatelliteSource.LEOSATDATA), clock.millis());
		satelliteDao.reindex();
		Satellite expected = satelliteDao.findById("42784");
		assertEquals(1601504221000L, expected.getLastUpdateTime());
		assertEquals(1, satelliteDao.findById("46494").getTransmitters().size());
	}

	@Test
	public void testLoadInstruments() {
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-noaa.json");
		satelliteDao = new SatelliteDao(config);
		Satellite sat = satelliteDao.findById("33591");
		assertNotNull(sat.getInstruments());
		assertEquals(6, sat.getInstruments().size());
		Instrument avhrr3 = findById(sat, "AVHRR3");
		assertNotNull(avhrr3);
		assertTrue(avhrr3.isPrimary());
		assertNotNull(avhrr3.getChannels());

		// test server-side can override instruments
		satelliteDao.saveLeosatdata(SatelliteDao.loadFromClasspathConfig("satellites-leosatdata-older-noaa.json", SatelliteSource.LEOSATDATA), clock.millis());
		satelliteDao.reindex();
		sat = satelliteDao.findById("33591");
		avhrr3 = findById(sat, "AVHRR3");
		assertFalse(avhrr3.isEnabled());
	}

	@Before
	public void start() throws Exception {
		clock = new FixedClock(TestUtil.getTime("2020-09-30 22:17:01.000"));
		config = new TestConfiguration(tempFolder, FileSystems.getDefault());
		config.setProperty("locaiton.lat", "51.49");
		config.setProperty("locaiton.lon", "0.01");
		config.setProperty("r2cloud.newLaunches", true);
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		config.setProperty("satnogs.satellites", true);
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.json").getAbsolutePath());
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test-schedule.json"); // just smaller number of satellites
		config.setProperty("satellites.custom.location", "./src/test/resources/satellites-custom.json");
		config.setProperty("satellites.leosatdata.location", new File(tempFolder.getRoot(), "leosatdata.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.new.location", new File(tempFolder.getRoot(), "leosatdata.new.json").getAbsolutePath());
		config.setProperty("satellites.satnogs.location", new File(tempFolder.getRoot(), "satnogs.json").getAbsolutePath());
		config.update();

		satelliteDao = new SatelliteDao(config);
	}

	private static Instrument findById(Satellite sat, String id) {
		for (Instrument cur : sat.getInstruments()) {
			if (cur.getId().equals(id)) {
				return cur;
			}
		}
		return null;
	}

}
