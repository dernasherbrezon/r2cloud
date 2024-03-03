package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.DefaultClock;

public class CelestrakClientTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private CelestrakServer server;
	private TestConfiguration config;

	@Test
	public void testSuccess() {
		String expectedBody = TestUtil.loadExpected("sample-tle.txt");
		Map<String, Tle> expected = convert(expectedBody);
		server.mockResponse(expectedBody);
		
		// one slash is important here
		CelestrakClient client = new CelestrakClient(config, new DefaultClock());
		Map<String, Tle> actual = client.downloadTle();
		assertEquals(expected.size(), actual.size());
		assertEquals(expected, actual);
	}

	@Test
	public void testFailure() {
		CelestrakClient client = new CelestrakClient(config, new DefaultClock());
		assertEquals(0, client.downloadTle().size());
	}

	private static Map<String, Tle> convert(String body) {
		Map<String, Tle> result = new HashMap<>();
		String[] lines = body.split("\n");
		for (int i = 0; i < lines.length; i += 3) {
			result.put(lines[i + 2].substring(2, 2 + 5).trim(), new Tle(new String[] { lines[i], lines[i + 1], lines[i + 2] }));
		}
		return result;
	}

	@Before
	public void start() throws Exception {
		server = new CelestrakServer();
		server.start();
		
		config = new TestConfiguration(tempFolder);
		config.setList("tle.urls", server.getUrls());
	}

	@After
	public void stop() throws Exception {
		server.stop();
	}

}
