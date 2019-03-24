package ru.r2cloud.it.util;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.R2Cloud;
import ru.r2cloud.Util;
import ru.r2cloud.it.AccessTokenIT;
import ru.r2cloud.it.ConfiguredIT;
import ru.r2cloud.it.GeneralIT;
import ru.r2cloud.it.ObservationIT;
import ru.r2cloud.it.R2CloudSaveIT;
import ru.r2cloud.it.RestoreIT;
import ru.r2cloud.it.ScheduleListIT;
import ru.r2cloud.it.ScheduleSaveIT;
import ru.r2cloud.it.SetupIT;
import ru.r2cloud.it.StaticControllerIT;
import ru.r2cloud.it.TleIT;
import ru.r2cloud.util.Configuration;

@RunWith(Suite.class)
@SuiteClasses({ ObservationIT.class, TleIT.class, StaticControllerIT.class, SetupIT.class, ScheduleSaveIT.class, ScheduleListIT.class, R2CloudSaveIT.class, RestoreIT.class, AccessTokenIT.class, ConfiguredIT.class, GeneralIT.class })
public class WebTest {

	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int MAX_RETRIES = 5;
	private static final Logger LOG = LoggerFactory.getLogger(WebTest.class);

	private static R2Cloud server;
	private static CelestrakServer celestrak;
	private static String userSettingsLocation;
	private static File rtlSdrMock;

	@BeforeClass
	public static void start() throws IOException {
		celestrak = new CelestrakServer();
		celestrak.start();
		celestrak.mockResponse(Util.loadExpected("sample-tle.txt"));

		rtlSdrMock = setupRtlSdrMock();

		userSettingsLocation = System.getProperty("java.io.tmpdir") + File.separator + ".r2cloud-" + UUID.randomUUID().toString();
		Configuration config;
		try (InputStream is = WebTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation);
		}
		config.setProperty("celestrak.hostname", celestrak.getUrl());
		config.setProperty("locaiton.lat", "56.189");
		config.setProperty("locaiton.lon", "38.174");
		config.setProperty("satellites.rtlsdr.path", rtlSdrMock.getAbsolutePath());

		server = new R2Cloud(config);
		server.start();
		assertStarted();
	}

	private static File setupRtlSdrMock() throws IOException {
		String rtlSdrMock = "rtl_sdr_mock.sh";
		File rtlSdrMockFile = new File(System.getProperty("java.io.tmpdir") + File.separator + rtlSdrMock);
		try (BufferedReader r = new BufferedReader(new InputStreamReader(WebTest.class.getClassLoader().getResourceAsStream(rtlSdrMock), StandardCharsets.UTF_8)); BufferedWriter w = new BufferedWriter(new FileWriter(rtlSdrMockFile))) {
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				w.append(curLine).append("\n");
			}
		}
		rtlSdrMockFile.setExecutable(true);
		return rtlSdrMockFile;
	}

	@AfterClass
	public static void stop() {
		if (server != null) {
			server.stop();
		}
		if (userSettingsLocation != null) {
			if (!new File(userSettingsLocation).delete()) {
				LOG.error("unable to delete temp user settings: {}", userSettingsLocation);
			}
		}
		if (rtlSdrMock != null && rtlSdrMock.exists() && !rtlSdrMock.delete()) {
			LOG.error("unable to delete rtlsdr mock at: {}", rtlSdrMock.getAbsolutePath());
		}
		if (celestrak != null) {
			celestrak.stop();
		}
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

}
