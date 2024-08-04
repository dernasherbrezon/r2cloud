package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
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
import ru.r2cloud.satellite.PriorityService;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.DefaultClock;
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
	private PriorityService priorityService;
	private Map<String, Tle> tleData;
	private Housekeeping dao;

	@Test
	public void testReloadTleForNewSatellites() throws Exception {
		config.setProperty("r2cloud.apiKey", UUID.randomUUID().toString());
		config.setProperty("r2cloud.newLaunches", true);
		dao = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);

		String id = "R2CLOUD001";
		List<Satellite> newLaunches = Collections.singletonList(createNewLaunch(id));
		when(leosatdata.loadNewLaunches(anyLong())).thenReturn(newLaunches);

		// call leosatdata and setup caches
		dao.run();

		Satellite sat = satelliteDao.findById(id);
		assertNotNull(sat);
		assertNotNull(sat.getTle());

		// this will force load from the disk
		// TLE is not saved onto disk
		satelliteDao = new SatelliteDao(config);
		dao = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);
		// this will setup tle cache
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

		dao = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);
		dao.run();
		assertNotNull(sat.getTle());
	}

	@Test
	public void testTleUpdate() throws Exception {
		Housekeeping reloader = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);
		reloader.run();
		Satellite meteor = satelliteDao.findById("40069");
		assertNotNull(meteor);
		assertNotNull(meteor.getTle());
		assertEquals(meteor.getTle().getRaw()[1], "1 40069U 14037A   18286.52491495 -.00000023  00000-0  92613-5 0  9990");
		assertEquals(meteor.getTle().getRaw()[2], "2 40069  98.5901 334.4030 0004544 256.4188 103.6490 14.20654800221188");

		config.setProperty("housekeeping.tle.periodMillis", "-10000");

		Tle newTle = new Tle(new String[] { "METEOR M-2", "1 40069U 14037A   24217.61569736  .00000303  00000-0  15898-3 0  9993", "2 40069  98.4390 209.6955 0005046 206.9570 153.1346 14.21009510522504" });
		tleData.put("40069", newTle);

		reloader.run();

		meteor = satelliteDao.findById("40069");
		assertNotNull(meteor);
		assertNotNull(meteor.getTle());
		assertEquals(meteor.getTle().getRaw()[1], newTle.getRaw()[1]);
		assertEquals(meteor.getTle().getRaw()[2], newTle.getRaw()[2]);
	}

	@Test
	public void testSuccess() throws Exception {
		Housekeeping reloader = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);
		reloader.start();

		verify(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Test
	public void testLifecycle() {
		Housekeeping reloader = new Housekeeping(config, satelliteDao, threadPool, celestrak, tleDao, null, leosatdata, null, priorityService);
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
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.location", new File(tempFolder.getRoot(), "leosatdata.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.new.location", new File(tempFolder.getRoot(), "leosatdata.new.json").getAbsolutePath());
		config.setProperty("satellites.satnogs.location", new File(tempFolder.getRoot(), "satnogs.json").getAbsolutePath());
		config.update();

		satelliteDao = new SatelliteDao(config);

		celestrak = mock(CelestrakClient.class);
		when(celestrak.downloadTle()).thenReturn(tleData);
		tleDao = new TleDao(config);

		priorityService = new PriorityService(config, new DefaultClock());

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

	private static Satellite createNewLaunch(String id) {
		Satellite result = create(id);
		result.setTle(new Tle(new String[] { result.getName(), "1 00001U 17036V   22114.09726310  .00014064  00000+0  52305-3 0  9992", "2 00001  97.2058 157.8198 0011701 152.8519 207.3335 15.27554982268713" }));
		return result;
	}

	@After
	public void stop() {
		if (dao != null) {
			dao.stop();
		}
	}
}
