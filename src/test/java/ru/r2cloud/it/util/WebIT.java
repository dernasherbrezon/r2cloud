package ru.r2cloud.it.util;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.it.SetupIT;
import ru.r2cloud.it.TleIT;

@RunWith(Suite.class)
@SuiteClasses({ SetupIT.class, TleIT.class })
public class WebIT {

	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int MAX_RETRIES = 5;
	private static final Logger LOG = LoggerFactory.getLogger(WebIT.class);

	@BeforeClass
	public static void start() {
		assertStarted();
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
		try (RestClient client = new RestClient(System.getProperty("r2cloud.baseurl"))) {
			return client.healthy();
		} catch (Exception e) {
			LOG.error("unable to get status", e);
			return false;
		}
	}

}
