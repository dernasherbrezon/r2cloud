package ru.r2cloud.it.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.CollectingRequestHandler;
import ru.r2cloud.FixedClock;
import ru.r2cloud.JsonHttpResponse;
import ru.r2cloud.R2Cloud;
import ru.r2cloud.RotctrldMock;
import ru.r2cloud.RtlTestServer;
import ru.r2cloud.SatnogsServerMock;
import ru.r2cloud.TestUtil;
import ru.r2cloud.satellite.reader.SpyServerMock;
import ru.r2cloud.satellite.reader.SpyServerReaderTest;
import ru.r2cloud.util.Configuration;

public abstract class BaseTest {

	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int MAX_RETRIES = 5;
	private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);
	public static final int ROTCTRLD_PORT = 8004;
	private static final int ROTCTRLD_PORT_LORA = 8006;
	private static final int LORA_AT_WIFI_PORT = 8005;

	protected R2Cloud server;
	private CelestrakServer celestrak;
	private File rtlSdrMock;
	private File rtlTestMock;
	private File plutoSdrTestMock;
	private RtlTestServer rtlTestServer;
	private RtlTestServer plutoTestServer;
	private RotctrldMock rotctrlMock;
	private RotctrldMock rotctrlMockForLora;
	private SatnogsServerMock satnogs;
	private SpyServerMock spyServerMock;
	protected HttpServer loraAtWifiServer;
	protected String unixFile = "/tmp/system_dbus_r2cloud_test_" + Math.abs(new Random().nextInt());

	protected RestClient client;

	protected String username = "info@r2cloud.ru";
	protected String password = "1";
	protected String keyword = "ittests";

	protected Configuration config;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File tempDirectory;

	@Before
	public void start() throws Exception {
		LogManager.getLogManager().reset();
		tempDirectory = new File(tempFolder.getRoot(), "tmp");
		if (!tempDirectory.mkdirs()) {
			throw new RuntimeException("unable to create temp dir: " + tempDirectory.getAbsolutePath());
		}
		celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(TestUtil.loadExpected("sample-tle.txt"));

		satnogs = new SatnogsServerMock();
		satnogs.start();
		satnogs.setSatellitesMock("[]", 200);
		satnogs.setTransmittersMock("[]", 200);

		rtlTestServer = new RtlTestServer(8003);
		rtlTestServer.mockDefault();
		rtlTestServer.start();

		plutoTestServer = new RtlTestServer(8010);
		plutoTestServer.mockTest(
				"Using auto-detected IIO context at URI \"usb:0.1.5\"\nIIO context created with usb backend.\nBackend description string: Linux (none) 4.19.0-119999-g6edc6cd #319 SMP PREEMPT Mon Jul 6 15:45:01 CEST 2020 armv7l\nIIO context has 15 attributes:\n	hw_model: Analog Devices PlutoSDR Rev.B (Z7010-AD9363A)\n	hw_model_variant: 0\n	hw_serial: 10447354119600050d003000d4311fd131\n");
		plutoTestServer.start();

		spyServerMock = new SpyServerMock("127.0.0.1");
		spyServerMock.setDeviceInfo(SpyServerReaderTest.createAirSpy());
		spyServerMock.setSync(SpyServerReaderTest.createValidSync());
		spyServerMock.start();

		rotctrlMock = new RotctrldMock(ROTCTRLD_PORT);
		rotctrlMock.setHandler(new CollectingRequestHandler("RPRT 0\n"));
		rotctrlMock.start();

		rotctrlMockForLora = new RotctrldMock(ROTCTRLD_PORT_LORA);
		rotctrlMockForLora.setHandler(new CollectingRequestHandler("RPRT 0\n"));
		rotctrlMockForLora.start();

		loraAtWifiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", LORA_AT_WIFI_PORT), 0);
		loraAtWifiServer.createContext("/api/v2/status", new JsonHttpResponse("loraatwifitest/status.json", 200));
		loraAtWifiServer.start();

		rtlSdrMock = TestUtil.setupScript(new File(tempFolder.getRoot(), "rtl_sdr_mock.sh"));
		rtlTestMock = TestUtil.setupScript(new File(tempFolder.getRoot(), "rtl_test_mock.sh"));
		plutoSdrTestMock = TestUtil.setupScript(new File(tempFolder.getRoot(), "iio_info_mock.sh"));

		config = prepareConfiguration();
		config.update();

		server = new R2Cloud(config, new FixedClock(1559942730784L));
		server.start();
		assertStarted();

		client = new RestClient(System.getProperty("r2cloud.baseurl"));
	}

	protected Configuration prepareConfiguration() throws IOException {
		File userSettingsLocation = new File(tempFolder.getRoot(), ".r2cloud-" + UUID.randomUUID().toString());
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-user-test.properties"); FileOutputStream fos = new FileOutputStream(userSettingsLocation)) {
			Properties props = new Properties();
			props.load(is);
			props.store(fos, "");
		}
		Configuration config;
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		config.setProperty("tle.urls", celestrak.getUrlsAsProperty());
		config.setProperty("tle.cacheFileLocation", new File(tempFolder.getRoot(), "tle.txt").getAbsolutePath());
		config.setProperty("satellites.meta.location", "./src/test/resources/satellites-test.json");
		config.setProperty("satellites.satnogs.location", new File(tempFolder.getRoot(), "satnogs.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.location", new File(tempFolder.getRoot(), "leosatdata.json").getAbsolutePath());
		config.setProperty("satellites.leosatdata.new.location", new File(tempFolder.getRoot(), "leosatdata.new.json").getAbsolutePath());
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");
		config.setProperty("satellites.rtlsdr.path", rtlSdrMock.getAbsolutePath());
		config.setProperty("satellites.rtlsdr.test.path", rtlTestMock.getAbsolutePath());
		config.setProperty("satellites.plutosdr.test.path", plutoSdrTestMock.getAbsolutePath());
		config.setProperty("satellites.sox.path", "sox");
		config.setProperty("leosatdata.hostname", "http://localhost:8001");
		config.setProperty("satnogs.hostname", satnogs.getUrl());
		config.setProperty("server.tmp.directory", tempDirectory.getAbsolutePath());
		config.setProperty("server.static.location", tempFolder.getRoot().getAbsolutePath() + File.separator + "data");
		config.setProperty("metrics.basepath.location", tempFolder.getRoot().getAbsolutePath() + File.separator + "data" + File.separator + "rrd");
		config.setProperty("auto.update.basepath.location", tempFolder.getRoot().getAbsolutePath() + File.separator + "data" + File.separator + "auto-udpate");
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath() + File.separator + "data" + File.separator + "satellites");
		config.setProperty("satellites.wxtoimg.license.path", tempFolder.getRoot().getAbsolutePath() + File.separator + "data" + File.separator + "wxtoimg" + File.separator + ".wxtoimglic");
		System.setProperty("DBUS_SYSTEM_BUS_ADDRESS", "unix:path=" + unixFile);
		File setupKeyword = new File(tempFolder.getRoot(), "r2cloud.txt");
		try (Writer w = new FileWriter(setupKeyword)) {
			w.append("ittests");
		}
		config.setProperty("server.keyword.location", setupKeyword.getAbsolutePath());
		Path p = config.getPathFromProperty("satellites.meta.location");
		Files.setLastModifiedTime(p, FileTime.from(1719525695573L, TimeUnit.MILLISECONDS));
		return config;
	}

	@After
	public void stop() {
		if (server != null) {
			server.stop();
		}
		if (rtlSdrMock != null && rtlSdrMock.exists() && !rtlSdrMock.delete()) {
			LOG.error("unable to delete rtlsdr mock at: {}", rtlSdrMock.getAbsolutePath());
		}
		if (rtlTestMock != null && rtlTestMock.exists() && !rtlTestMock.delete()) {
			LOG.error("unable to delete rtltest mock at: {}", rtlTestMock.getAbsolutePath());
		}
		if (celestrak != null) {
			celestrak.stop();
		}
		if (plutoTestServer != null) {
			plutoTestServer.stop();
		}
		if (rtlTestServer != null) {
			rtlTestServer.stop();
		}
		if (rotctrlMock != null) {
			rotctrlMock.stop();
		}
		if (spyServerMock != null) {
			spyServerMock.stop();
		}
		if (rotctrlMockForLora != null) {
			rotctrlMockForLora.stop();
		}
		if (loraAtWifiServer != null) {
			loraAtWifiServer.stop(0);
		}
		if (satnogs != null) {
			satnogs.stop();
		}
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void assertErrorInField(String field, HttpResponse<String> response) {
		JsonObject result = (JsonObject) Json.parse(response.body());
		JsonObject errors = (JsonObject) result.get("errors");
		assertNotNull(errors);
		assertNotNull(errors.get(field));
	}

	static void assertStarted() {
		int currentRetry = 0;
		while (currentRetry < MAX_RETRIES) {
			if (healthy()) {
				LOG.info("healthy");
				return;
			}
			LOG.info("not healthy yet");
			currentRetry++;
			try {
				Thread.sleep(RETRY_INTERVAL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		fail("not healthy within timeout " + RETRY_INTERVAL_MS + " and max retries " + MAX_RETRIES);
	}

	private static boolean healthy() {
		RestClient client;
		try {
			client = new RestClient(System.getProperty("r2cloud.baseurl"));
			return client.healthy();
		} catch (Exception e) {
			return false;
		}

	}

	public void assertTempEmpty() {
		assertTrue(tempDirectory.isDirectory());
		File[] contents = tempDirectory.listFiles();
		assertEquals("expected empty, but got: " + contents.length, 0, contents.length);
	}

}
