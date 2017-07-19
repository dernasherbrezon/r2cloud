package ru.r2cloud.uitl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Test;

public class DailyRollingFileHandlerTest {

	@Test
	public void testRollOver() throws Exception {
		try (InputStream is = DailyRollingFileHandlerTest.class.getClassLoader().getResourceAsStream("logging-test.properties")) {
			LogManager.getLogManager().readConfiguration(is);
		}
		DailyRollingFileHandler fh = new DailyRollingFileHandler();

		Logger logger = Logger.getLogger("test");
		logger.addHandler(fh);

		String message1 = UUID.randomUUID().toString();
		logger.info(message1);

		Calendar previousDay = Calendar.getInstance();
		previousDay.add(Calendar.DAY_OF_MONTH, -1);
		fh.setTime(previousDay);

		String message2 = UUID.randomUUID().toString();
		logger.info(message2);

		assertLineExist("message not exist in previous day file", message1, new File("logtest/r2cloud.log." + new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
		assertLineExist("message not exist in current day file ", message2, new File("logtest/r2cloud.log"));

	}

	private static void assertLineExist(String message, String expected, File actual) throws Exception {
		assertTrue(actual.exists());
		try (BufferedReader fr = new BufferedReader(new FileReader(actual))) {
			String curLine = null;
			while ((curLine = fr.readLine()) != null) {
				if (curLine.contains(expected)) {
					return;
				}
			}
		}
		fail(message);
	}

	@After
	public void stop() {
		File[] files = new File("logtest").listFiles();
		for (File cur : files) {
			cur.delete();
		}
	}

}
