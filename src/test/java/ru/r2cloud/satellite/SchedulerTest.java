package ru.r2cloud.satellite;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.ThreadPoolFactory;
import uk.me.g4dpz.satellite.SatPos;

public class SchedulerTest {

	private TestConfiguration config;
	private SatelliteDao satelliteDao;
	private ObservationFactory factory;
	private ThreadPoolFactory threadPool;
	private ScheduledExecutorService executor;
	private Observation observation;
	private Clock clock;
	private String id;

	@Test
	public void testSuccess() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss, SSS");
		Date current = sdf.parse("2017-10-23 00:00:00, 000");
		Date start = sdf.parse("2017-10-23 07:00:00, 000");
		Date end = sdf.parse("2017-10-23 08:00:00, 000");
		when(observation.getNextPass()).thenReturn(create(start, end));
		when(clock.millis()).thenReturn(current.getTime());
		Scheduler s = new Scheduler(config, satelliteDao, new RtlSdrLock(), factory, threadPool, clock);
		s.start();

		verify(executor).schedule(any(), eq(TimeUnit.HOURS.toMillis(7)), eq(TimeUnit.MILLISECONDS));
		verify(executor).schedule(any(), eq(TimeUnit.HOURS.toMillis(8)), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	public void testListenToConfiguration() throws Exception {
		config.setProperty("satellites.enabled", false);
		Scheduler s = new Scheduler(config, satelliteDao, new RtlSdrLock(), factory, threadPool, clock);
		s.start();

		verify(executor, never()).schedule(any(), anyLong(), any());

		config.setProperty("satellites.enabled", true);
		config.update();

		verify(executor, times(2)).schedule(any(), anyLong(), any());
	}

	@Test
	public void testLifecycle() {
		Scheduler s = new Scheduler(config, satelliteDao, new RtlSdrLock(), factory, threadPool, clock);
		s.start();
		s.start();

		verify(executor, times(2)).schedule(any(), anyLong(), any());
		assertNotNull(s.getNextObservation(id));

		s.stop();
		s.stop();
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration();
		config.setProperty("satellites.enabled", true);

		clock = mock(Clock.class);
		id = UUID.randomUUID().toString();
		satelliteDao = mock(SatelliteDao.class);
		factory = mock(ObservationFactory.class);
		observation = mock(Observation.class);
		threadPool = mock(ThreadPoolFactory.class);
		executor = mock(ScheduledExecutorService.class);
		when(threadPool.newScheduledThreadPool(anyInt(), any())).thenReturn(executor);
		when(factory.create(any(), any())).thenReturn(observation);
		when(observation.getNextPass()).thenReturn(create(new Date(), new Date()));
		when(satelliteDao.findSupported()).thenReturn(Collections.singletonList(createSatellite(id)));
		when(executor.awaitTermination(anyLong(), any())).thenReturn(true);
	}

	private static SatPass create(Date start, Date end) {
		SatPos startPos = new SatPos();
		startPos.setTime(start);
		SatPos endPos = new SatPos();
		endPos.setTime(end);
		SatPass result = new SatPass();
		result.setStart(startPos);
		result.setEnd(endPos);
		return result;
	}

	private static Satellite createSatellite(String id) {
		Satellite result = new Satellite();
		result.setId(id);
		return result;
	}

}
