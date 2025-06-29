package ru.r2cloud.it;

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
		plutoTestServer.mockTest(
				"Using auto-detected IIO context at URI \"usb:0.1.5\"\nIIO context created with usb backend.\nBackend description string: Linux (none) 4.19.0-119999-g6edc6cd #319 SMP PREEMPT Mon Jul 6 15:45:01 CEST 2020 armv7l\nIIO context has 15 attributes:\n	hw_model: Analog Devices PlutoSDR Rev.B (Z7010-AD9363A)\n	hw_model_variant: 0\n	hw_serial: 10447354119600050d003000d4311fd131\n");
		plutoTestServer.start();

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
		if (loraAtWifiServer != null) {
			loraAtWifiServer.stop(0);
		}
	}
}
