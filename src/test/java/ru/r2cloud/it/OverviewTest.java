package ru.r2cloud.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.RtlTestServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.satellite.reader.SpyServerMock;
import ru.r2cloud.satellite.reader.SpyServerReaderTest;
import ru.r2cloud.util.Configuration;

public class OverviewTest extends RegisteredTest {

	private SpyServerMock spyServerMock;
	private RtlTestServer plutoTestServer;
	private HttpServer loraAtWifiServer;
	private RtlTestServer airspyTestServer;

	@Test
	public void testSuccess() {
		JsonObject overview = client.getOverview();
		// time is unstable
		overview.remove("serverTime");
		TestUtil.assertJson("expectedOverview.json", overview);
	}

	@Override
	protected Configuration prepareConfiguration() throws IOException {
		Configuration result = super.prepareConfiguration();
		try (InputStream is = OverviewTest.class.getClassLoader().getResourceAsStream("config-all-devices.properties")) {
			Properties props = new Properties();
			props.load(is);
			for (Entry<Object, Object> cur : props.entrySet()) {
				result.setProperty(cur.getKey().toString(), cur.getValue().toString());
			}
		}
		File airspyInfoMock = TestUtil.setupScript(new File(tempFolder.getRoot(), "airspy_info_mock.sh"));
		result.setProperty("satellites.airspy_info.path", airspyInfoMock.getAbsolutePath());

		File plutoSdrTestMock = TestUtil.setupScript(new File(tempFolder.getRoot(), "iio_info_mock.sh"));
		result.setProperty("satellites.plutosdr.test.path", plutoSdrTestMock.getAbsolutePath());
		return result;
	}

	@Before
	@Override
	public void start() throws Exception {
		spyServerMock = new SpyServerMock("127.0.0.1");
		spyServerMock.setDeviceInfo(SpyServerReaderTest.createAirSpy());
		spyServerMock.setSync(SpyServerReaderTest.createValidSync());
		spyServerMock.start();

		plutoTestServer = new RtlTestServer(8010);
		// @formatter:off
		plutoTestServer.mockTest(
				  "Using auto-detected IIO context at URI \"usb:0.1.5\"\n"
				+ "IIO context created with usb backend.\n"
				+ "Backend description string: Linux (none) 4.19.0-119999-g6edc6cd #319 SMP PREEMPT Mon Jul 6 15:45:01 CEST 2020 armv7l\n"
				+ "IIO context has 15 attributes:\n"
				+ "hw_model: Analog Devices PlutoSDR Rev.B (Z7010-AD9363A)\n"
				+ "	hw_model_variant: 0\n"
				+ "	hw_serial: 10447354119600050d003000d4311fd131\n");
		// @formatter:on
		plutoTestServer.start();

		airspyTestServer = new RtlTestServer(8011);
		// @formatter:off
		airspyTestServer.mockTest("airspy_lib_version: 1.0.10\n"
				+ "\n"
				+ "Found AirSpy board 1\n"
				+ "Board ID Number: 0 (AIRSPY)\n"
				+ "Firmware Version: AirSpy MINI cf1a374-dirty 2021-10-06\n"
				+ "Part ID Number: 0x6906002B 0x00000030\n"
				+ "Serial Number: 0x62CC68FF21317A17\n"
				+ "Supported sample rates:\n"
				+ "	6.000000 MSPS\n"
				+ "	3.000000 MSPS\n"
				+ "	2.400000 MSPS\n");
		// @formatter:on
		airspyTestServer.start();

		loraAtWifiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8005), 0);
		loraAtWifiServer.createContext("/api/v2/status", new JsonHttpResponse("loraatwifitest/status.json", 200));
		loraAtWifiServer.start();

		super.start();
	}

	@After
	@Override
	public void stop() {
		super.stop();
		if (spyServerMock != null) {
			spyServerMock.stop();
		}
		if (plutoTestServer != null) {
			plutoTestServer.stop();
		}
		if (airspyTestServer != null) {
			airspyTestServer.stop();
		}
		if (loraAtWifiServer != null) {
			loraAtWifiServer.stop(0);
		}
	}
}
