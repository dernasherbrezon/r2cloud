package ru.r2cloud.sdr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.RtlTestServer;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;

public class PlutoStatusProcessTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Configuration config;
	private PlutoStatusProcess status;
	private RtlTestServer rtlTestServer;

	@Test
	public void testUnknownInput() {
		rtlTestServer.mockTest(UUID.randomUUID().toString());
		SdrStatus result = status.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, result.getStatus());
	}

	@Test
	public void testNoDevices() {
		rtlTestServer.mockTest("Library version: 0.21 (git tag: 565bf68)\nCompiled with backends: xml ip usb\nNo IIO context found.");
		SdrStatus result = status.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, result.getStatus());
		assertEquals("No IIO context found.", result.getFailureMessage());
	}

	@Test
	public void testSuccess() {
		rtlTestServer.mockTest(
				"Using auto-detected IIO context at URI \"usb:0.1.5\"\nIIO context created with usb backend.\nBackend description string: Linux (none) 4.19.0-119999-g6edc6cd #319 SMP PREEMPT Mon Jul 6 15:45:01 CEST 2020 armv7l\nIIO context has 15 attributes:\n	hw_model: Analog Devices PlutoSDR Rev.B (Z7010-AD9363A)\n	hw_model_variant: 0\n	hw_serial: 10447354119600050d003000d4311fd131\n");
		SdrStatus result = status.getStatus();
		assertEquals(DeviceConnectionStatus.CONNECTED, result.getStatus());
		assertEquals("Analog Devices PlutoSDR Rev.B (Z7010-AD9363A)", result.getModel());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.plutosdr.test.path", TestUtil.setupScript(new File(tempFolder.getRoot().getAbsoluteFile(), "iio_info_mock.sh")).getAbsolutePath());
		config.update();

		rtlTestServer = new RtlTestServer();
		rtlTestServer.start();

		status = new PlutoStatusProcess(config, new ProcessFactory());
	}

	@After
	public void stop() {
		if (rtlTestServer != null) {
			rtlTestServer.stop();
		}
	}
}
