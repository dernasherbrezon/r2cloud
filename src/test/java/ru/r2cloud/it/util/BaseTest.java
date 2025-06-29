package ru.r2cloud.it.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.util.Random;
import java.util.UUID;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.FixedClock;
import ru.r2cloud.R2Cloud;
import ru.r2cloud.RtlTestServer;
import ru.r2cloud.SatnogsServerMock;
import ru.r2cloud.TestUtil;
import ru.r2cloud.util.Configuration;

public abstract class BaseTest {

	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int MAX_RETRIES = 5;
	private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

	protected R2Cloud server;
	private CelestrakServer celestrak;
	private File rtlSdrMock;
	private File rtlTestMock;
	private File plutoSdrTestMock;
	private RtlTestServer rtlTestServer;
	private SatnogsServerMock satnogs;

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
		config.setProperty("satellits.validate.external", false);
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
		if (rtlTestServer != null) {
			rtlTestServer.stop();
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
