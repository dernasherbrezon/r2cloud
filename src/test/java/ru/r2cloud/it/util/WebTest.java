package ru.r2cloud.it.util;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.it.AccessTokenIT;
import ru.r2cloud.it.ConfiguredIT;
import ru.r2cloud.it.GeneralConfigurationIT;
import ru.r2cloud.it.R2CloudSaveIT;
import ru.r2cloud.it.RestoreIT;
import ru.r2cloud.it.SetupIT;

@RunWith(Suite.class)
@SuiteClasses({ SetupIT.class, R2CloudSaveIT.class, RestoreIT.class, AccessTokenIT.class, ConfiguredIT.class, GeneralConfigurationIT.class })
public class WebTest {

	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int MAX_RETRIES = 5;
	private static final Logger LOG = LoggerFactory.getLogger(WebTest.class);

	private static R2Cloud server;

	@BeforeClass
	public static void start() throws IOException {
		try (InputStream is = WebTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			server = new R2Cloud(is);
		}
		server.start();
		assertStarted();
	}

	@AfterClass
	public static void stop() {
		if (server != null) {
			server.stop();
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
