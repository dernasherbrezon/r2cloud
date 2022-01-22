package ru.r2cloud;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.sdr.SdrStatusDao;
import ru.r2cloud.util.ProcessFactory;

public class SdrStatusDaoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private int expectedRtlDeviceId = 0;
	private TestConfiguration config;
	private SdrStatusDao dao;
	private RtlTestServer rtlTestServer;

	@Test
	public void testUnknown() {
		rtlTestServer.mockTest("No supported\n");
		SdrStatus result = dao.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, result.getStatus());
	}

	@Test
	public void testUnknownOutput() {
		rtlTestServer.mockTest(UUID.randomUUID().toString() + "\n");
		SdrStatus result = dao.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, result.getStatus());
	}

	@Test
	public void testSuccess() {
		rtlTestServer.mockTest("  0:  Realtek, RTL2838UHIDIR, SN: 00000001\n");
		SdrStatus result = dao.getStatus();
		assertEquals(DeviceConnectionStatus.CONNECTED, result.getStatus());
		assertEquals("Realtek, RTL2838UHIDIR, SN: 00000001", result.getModel());
	}

	@Test
	public void testAssertMultipleDongles() {
		rtlTestServer.mockTest("  1:  Realtek, RTL2838UHIDIR, SN: 00000002\n  0:  Realtek, RTL2838UHIDIR, SN: 00000001\n");
		SdrStatus result = dao.getStatus();
		assertEquals(DeviceConnectionStatus.CONNECTED, result.getStatus());
		assertEquals("Realtek, RTL2838UHIDIR, SN: 00000001", result.getModel());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.rtlsdr.test.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "rtl_test_mock.sh")).getAbsolutePath());
		config.setProperty("satellites.sdr", "rtlsdr");
		config.update();

		rtlTestServer = new RtlTestServer();
		rtlTestServer.start();

		dao = new SdrStatusDao(config, new ProcessFactory(), expectedRtlDeviceId);
	}

	@After
	public void stop() {
		if (rtlTestServer != null) {
			rtlTestServer.stop();
		}
	}
}
