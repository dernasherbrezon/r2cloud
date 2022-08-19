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
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactory;

public class TLEReloaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private MockFileSystem fs;
	private Clock clock;
	private ThreadPoolFactory threadPool;
	private ScheduledExecutorService executor;
	private long current;
	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private CelestrakClient celestrak;
	private Map<String, Tle> tleData;
	private TLEReloader dao;

	@Test
	public void testReloadFailure() {
		String satelliteId = "40069";
		Satellite sat = satelliteDao.findById(satelliteId);
		assertNull(sat.getTle());

		dao = new TLEReloader(config, satelliteDao, threadPool, clock, celestrak);
		dao.reload();
		assertNotNull(sat.getTle());

		sat.setTle(null);
		Path failingPath = config.getSatellitesBasePath().resolve(satelliteId);
		fs.mock(failingPath, new FailingByteChannelCallback(10));
		dao.reload();
		fs.removeMock(failingPath);
		// even if filesystem failed check in memory TLE exist
		assertNotNull(sat.getTle());

		// reload from disk
		satelliteDao.reload();
		assertNotNull(satelliteDao.findById(satelliteId).getTle());
	}

	@Test
	public void testSuccess() throws Exception {
		TLEReloader reloader = new TLEReloader(config, satelliteDao, threadPool, clock, celestrak);
		reloader.start();

		verify(clock).millis();
		verify(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Test
	public void testLifecycle() {
		TLEReloader reloader = new TLEReloader(config, satelliteDao, threadPool, clock, celestrak);
		reloader.start();
		reloader.start();
		verify(executor, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Before
	public void start() throws Exception {
		tleData = new HashMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(TLEReloaderTest.class.getClassLoader().getResourceAsStream("sample-tle.txt")))) {
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

		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		satelliteDao = new SatelliteDao(config, null, null);

		celestrak = mock(CelestrakClient.class);
		when(celestrak.getTleForActiveSatellites()).thenReturn(tleData);
		
		clock = mock(Clock.class);
		threadPool = mock(ThreadPoolFactory.class);
		executor = mock(ScheduledExecutorService.class);
		when(threadPool.newScheduledThreadPool(anyInt(), any())).thenReturn(executor);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss, SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		current = sdf.parse("2017-10-23 00:00:00, 000").getTime();

		when(clock.millis()).thenReturn(current);

	}

	@After
	public void stop() {
		if (dao != null) {
			dao.stop();
		}
	}
}
