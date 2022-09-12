package ru.r2cloud.tle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteSource;
import ru.r2cloud.model.Tle;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.ThreadPoolFactory;

public class HousekeepingTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private ThreadPoolFactory threadPool;
	private ScheduledExecutorService executor;
	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private CelestrakClient celestrak;
	private LeoSatDataClient leosatdata;
	private TleDao tleDao;
	private Map<String, Tle> tleData;
	private Housekeeping dao;

	@Test
	public void testReloadTleForNewSatellites() throws Exception {
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		dao = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null);
		// this will setup tle cache
		dao.run();

		String id = "00001";
		satelliteDao.saveLeosatdata(Collections.singletonList(create(id)), System.currentTimeMillis());
		satelliteDao.reindex();
		Satellite sat = satelliteDao.findById(id);
		assertNotNull(sat);
		assertNull(sat.getTle());

		// this should reload tle even it was cached before
		dao.run();

		sat = satelliteDao.findById(id);
		assertNotNull(sat);
		assertNotNull(sat.getTle());
	}

	@Test
	public void testReloadFailure() {
		String satelliteId = "40069";
		Satellite sat = satelliteDao.findById(satelliteId);
		assertNull(sat.getTle());

		dao = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null);
		dao.run();
		assertNotNull(sat.getTle());
	}

	@Test
	public void testSuccess() throws Exception {
		Housekeeping reloader = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null);
		reloader.start();

		verify(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Test
	public void testLifecycle() {
		Housekeeping reloader = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null);
		reloader.start();
		reloader.start();
		verify(executor, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Before
	public void start() throws Exception {
		tleData = new HashMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(HousekeepingTest.class.getClassLoader().getResourceAsStream("sample-tle.txt")))) {
			String curLine = null;
			List<String> lines = new ArrayList<>();
			while ((curLine = r.readLine()) != null) {
				lines.add(curLine);
			}
			for (int i = 0; i < lines.size(); i += 3) {
				tleData.put(lines.get(i + 2).substring(2, 2 + 5).trim(), new Tle(new String[] { lines.get(i), lines.get(i + 1), lines.get(i + 2) }));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		config = new TestConfiguration(tempFolder, FileSystems.getDefault());
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.txt").getAbsolutePath());
		config.update();

		satelliteDao = new SatelliteDao(config);

		celestrak = mock(CelestrakClient.class);
		when(celestrak.downloadTle()).thenReturn(tleData);
		tleDao = new TleDao(config);

		threadPool = mock(ThreadPoolFactory.class);
		executor = mock(ScheduledExecutorService.class);
		when(threadPool.newScheduledThreadPool(anyInt(), any())).thenReturn(executor);

		leosatdata = mock(LeoSatDataClient.class);
		when(leosatdata.loadSatellites(anyLong())).thenReturn(Collections.emptyList());
	}

	private static Satellite create(String id) {
		Satellite result = new Satellite();
		result.setId(id);
		result.setName(UUID.randomUUID().toString());
		result.setPriority(Priority.NORMAL);
		result.setSource(SatelliteSource.LEOSATDATA);
		Transmitter transmitter = new Transmitter();
		transmitter.setFrequency(466000000);
		transmitter.setFraming(Framing.CUSTOM);
		result.setTransmitters(Collections.singletonList(transmitter));
		return result;
	}

	@After
	public void stop() {
		if (dao != null) {
			dao.stop();
		}
	}
}
