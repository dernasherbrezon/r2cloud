package ru.r2cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.health.HealthCheck.Result;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.metrics.Status;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ThreadPoolFactory;

public class RtlSdrStatusDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private RtlSdrStatusDao dao;
	private RtlTestServer rtlTestServer;
	private Metrics metrics;

	@Test
	public void testInitialStatus() {
		RtlSdrLock lock = new RtlSdrLock();
		dao = new RtlSdrStatusDao(config, lock, createNoOpThreadFactory(), metrics);
		lock.register(RtlSdrStatusDao.class, 1);
		dao.start();

		assertUnknown();
		assertPpm(0);
	}

	@Test
	public void testUnknown() {
		rtlTestServer.mockTest("No supported\n");
		rtlTestServer.mockPpm("No supported\n");
		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertUnknown(status.get("rtltest"));
		assertError(status.get("rtldongle"));
		assertPpm(0);
	}

	@Test
	public void testUnknownOutput() {
		rtlTestServer.mockTest(UUID.randomUUID().toString() + "\n");
		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertUnknown(status.get("rtltest"));
		assertUnknown(status.get("rtldongle"));
	}

	@Test
	public void testSuccess() {
		rtlTestServer.mockDefault();

		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertHealthy(status.get("rtltest"));
		assertHealthy(status.get("rtldongle"));
		assertPpm(53);
	}

	@Test
	public void shutdownProcessProperly() throws Exception {
		config.setProperty("rtltest.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "rtl_test_mock_timeouted.sh")).getAbsolutePath());
		config.setProperty("rtltest.interval.seconds", TimeUnit.HOURS.toSeconds(24));
		config.update();

		RtlSdrLock lock = new RtlSdrLock();
		dao = new RtlSdrStatusDao(config, lock, new ExecuteNowThreadFactory(false), metrics);
		lock.register(RtlSdrStatusDao.class, 1);
		dao.start();

		long startTerminationMillis = System.currentTimeMillis();
		dao.stop();
		long totalTerminationMillis = System.currentTimeMillis() - startTerminationMillis;
		dao = null;
		assertTrue("took: " + totalTerminationMillis, totalTerminationMillis < config.getThreadPoolShutdownMillis());
	}

	@SuppressWarnings("unchecked")
	private void assertPpm(int ppm) {
		Map<String, Metric> metricsData = metrics.getRegistry().getMetrics();
		assertEquals(ppm, ((Gauge<Integer>) metricsData.get("ppm")).getValue().intValue());
	}

	private void createExecuteNowRtlSdrDao() {
		RtlSdrLock lock = new RtlSdrLock();
		dao = new RtlSdrStatusDao(config, lock, new ExecuteNowThreadFactory(), metrics);
		lock.register(RtlSdrStatusDao.class, 1);
		dao.start();
	}

	private void assertUnknown() {
		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertUnknown(status.get("rtltest"));
		assertUnknown(status.get("rtldongle"));
	}

	private static void assertHealthy(Result check) {
		assertTrue(check.isHealthy());
		assertEquals(Status.SUCCESS, check.getDetails().get("status"));
	}

	private static void assertUnknown(Result check) {
		assertTrue(check.isHealthy());
		assertEquals(Status.UNKNOWN, check.getDetails().get("status"));
	}

	private static void assertError(Result check) {
		assertFalse(check.isHealthy());
		assertEquals(Status.ERROR, check.getDetails().get("status"));
	}

	private static ThreadPoolFactory createNoOpThreadFactory() {
		ThreadPoolFactory threadFactory = Mockito.mock(ThreadPoolFactory.class);
		ScheduledExecutorService scheduledExecutor = Mockito.mock(ScheduledExecutorService.class);
		try {
			Mockito.when(scheduledExecutor.awaitTermination(Mockito.anyLong(), Mockito.any())).thenReturn(true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		Mockito.when(threadFactory.newScheduledThreadPool(Mockito.anyInt(), Mockito.any())).thenReturn(scheduledExecutor);
		return threadFactory;
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("rtltest.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "rtl_test_mock.sh")).getAbsolutePath());
		config.setProperty("stdbuf.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "stdbuf_mock.sh")).getAbsolutePath());
		config.update();

		rtlTestServer = new RtlTestServer();
		rtlTestServer.start();

		metrics = new Metrics(config, null, new DefaultClock());
	}

	@After
	public void stop() {
		if (dao != null) {
			dao.stop();
		}
		if (rtlTestServer != null) {
			rtlTestServer.stop();
		}
		if (metrics != null) {
			metrics.stop();
		}
	}
}
