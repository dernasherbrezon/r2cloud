package ru.r2cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.codahale.metrics.health.HealthCheck.Result;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.metrics.Status;
import ru.r2cloud.sdr.SdrStatusDao;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ProcessFactory;

public class SdrStatusDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private SdrStatusDao dao;
	private RtlTestServer rtlTestServer;
	private Metrics metrics;

	@Test
	public void testInitialStatus() {
		dao = new SdrStatusDao(config, metrics, new ProcessFactory());
		dao.start();

		assertUnknown();
	}

	@Test
	public void testUnknown() {
		rtlTestServer.mockTest("No supported\n");
		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertError(status.get("rtldongle"));
	}

	@Test
	public void testUnknownOutput() {
		rtlTestServer.mockTest(UUID.randomUUID().toString() + "\n");
		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertUnknown(status.get("rtldongle"));
	}

	@Test
	public void testSuccess() {
		rtlTestServer.mockDefault();

		createExecuteNowRtlSdrDao();

		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
		assertHealthy(status.get("rtltest"));
		assertHealthy(status.get("rtldongle"));
	}

	@Test
	public void shutdownProcessProperly() throws Exception {
		config.setProperty("satellites.rtlsdr.test.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "rtl_test_mock_timeouted.sh")).getAbsolutePath());
		config.update();

		dao = new SdrStatusDao(config, metrics, new ProcessFactory());
		dao.start();

		long startTerminationMillis = System.currentTimeMillis();
		dao.stop();
		long totalTerminationMillis = System.currentTimeMillis() - startTerminationMillis;
		dao = null;
		assertTrue("took: " + totalTerminationMillis, totalTerminationMillis < config.getThreadPoolShutdownMillis());
	}

	private void createExecuteNowRtlSdrDao() {
		dao = new SdrStatusDao(config, metrics, new ProcessFactory());
		dao.start();
	}

	private void assertUnknown() {
		Map<String, Result> status = metrics.getHealthRegistry().runHealthChecks();
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

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.setProperty("satellites.rtlsdr.test.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "rtl_test_mock.sh")).getAbsolutePath());
		config.setProperty("stdbuf.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "stdbuf_mock.sh")).getAbsolutePath());
		config.setProperty("threadpool.shutdown.millis", "15000");
		config.setProperty("satellites.sdr", "rtlsdr");
		config.update();

		rtlTestServer = new RtlTestServer();
		rtlTestServer.start();

		metrics = new Metrics(config, new DefaultClock());
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
