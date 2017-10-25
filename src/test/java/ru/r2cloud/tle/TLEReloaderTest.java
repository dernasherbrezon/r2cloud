package ru.r2cloud.tle;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.text.SimpleDateFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactory;

public class TLEReloaderTest {

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss, SSS");

	private TLEDao tleDao;
	private Clock clock;
	private ThreadPoolFactory threadPool;
	private ScheduledExecutorService executor;
	private long current;
	private TestConfiguration config;

	@Test
	public void testSuccess() throws Exception {
		TLEReloader reloader = new TLEReloader(config, tleDao, threadPool, clock);
		reloader.start();

		verify(clock).millis();
		verify(executor).scheduleAtFixedRate(any(), eq(TimeUnit.DAYS.toMillis(6)), eq(TimeUnit.DAYS.toMillis(7)), eq(TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void testListenToConfiguration() throws Exception {
		config.setProperty("satellites.enabled", false);
		TLEReloader reloader = new TLEReloader(config, tleDao, threadPool, clock);
		reloader.start();
		
		verify(executor, never()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

		config.setProperty("satellites.enabled", true);
		config.update();
		
		verify(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}
	
	@Test
	public void testLifecycle() {
		TLEReloader reloader = new TLEReloader(config, tleDao, threadPool, clock);
		reloader.start();
		reloader.start();
		verify(executor, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration();
		config.setProperty("satellites.enabled", true);
		tleDao = mock(TLEDao.class);
		clock = mock(Clock.class);
		threadPool = mock(ThreadPoolFactory.class);
		executor = mock(ScheduledExecutorService.class);
		when(threadPool.newScheduledThreadPool(anyInt(), any())).thenReturn(executor);

		current = sdf.parse("2017-10-23 00:00:00, 000").getTime();

		when(clock.millis()).thenReturn(current);

	}
}
