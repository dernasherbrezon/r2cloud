package ru.r2cloud.tle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.TLE;
import ru.r2cloud.satellite.SatelliteDao;

public class TLEDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private CelestrakClient celestrak;

	private Map<String, TLE> tleData;
	private List<Satellite> supported;

	@Test
	public void testSuccess() {
		TLEDao dao = new TLEDao(config, satelliteDao, celestrak);
		dao.reload();
		assertNotNull(dao.findById(supported.get(0).getId()));
	}

	@Test
	public void testLifecycle() {
		TLEDao dao = new TLEDao(config, satelliteDao, celestrak);
		dao.start();
		assertNotNull(dao.findById(supported.get(0).getId()));
		dao.stop();
		assertNull(dao.findById(supported.get(0).getId()));
	}

	@Test
	public void testTolerateFailures() {
		// everything is loaded ok
		TLEDao dao = new TLEDao(config, satelliteDao, celestrak);
		dao.start();
		dao.stop();

		celestrak = mock(CelestrakClient.class);
		// return tle for completely different satellites
		HashMap<String, TLE> brokenTleData = new HashMap<String, TLE>();
		brokenTleData.put(UUID.randomUUID().toString(), tleData.get(supported.get(0).getName()));
		when(celestrak.getWeatherTLE()).thenReturn(brokenTleData);
		// trigger last modified
		File tle = new File(tempFolder.getRoot(), supported.get(0).getId() + File.separator + "tle.txt");
		tle.setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(17));

		dao = new TLEDao(config, satelliteDao, celestrak);
		dao.start();
		assertNotNull(dao.findById(supported.get(0).getId()));
		assertNotNull(dao.findById(supported.get(1).getId()));
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.enabled", true);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		setupMocks();

	}

	private void setupMocks() {
		tleData = new HashMap<String, TLE>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TLEDaoTest.class.getClassLoader().getResourceAsStream("sample-tle.txt")))) {
			String curLine = null;
			List<String> lines = new ArrayList<>();
			while ((curLine = r.readLine()) != null) {
				lines.add(curLine);
			}
			for (int i = 0; i < lines.size(); i += 3) {
				tleData.put(lines.get(i), new TLE(new String[] { lines.get(i), lines.get(i + 1), lines.get(i + 2) }));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		supported = new ArrayList<Satellite>();
		for (String cur : tleData.keySet()) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(UUID.randomUUID().toString());
			curSatellite.setName(cur);
			supported.add(curSatellite);
		}

		satelliteDao = mock(SatelliteDao.class);
		celestrak = mock(CelestrakClient.class);
		when(celestrak.getWeatherTLE()).thenReturn(tleData);
		for (Satellite cur : supported) {
			when(satelliteDao.findByName(cur.getName())).thenReturn(cur);
		}
		when(satelliteDao.findSupported()).thenReturn(supported);

	}
}
