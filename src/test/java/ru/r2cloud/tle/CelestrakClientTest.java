package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.TestUtil;
import ru.r2cloud.model.Tle;

public class CelestrakClientTest {

	private CelestrakServer server;

	@Test
	public void testSuccess() {
		String expectedBody = TestUtil.loadExpected("sample-tle.txt");
		Map<String, Tle> expected = convert(expectedBody);
		server.mockResponse(expectedBody);

		// one slash is important here
		CelestrakClient client = new CelestrakClient(server.getUrls());
		Map<String, Tle> actual = client.getTleForActiveSatellites();
		assertEquals(expected.size(), actual.size());
		assertEquals(expected, actual);
	}

	@Test
	public void testFailure() {
		CelestrakClient client = new CelestrakClient(server.getUrls());
		assertEquals(0, client.getTleForActiveSatellites().size());
	}

	private static Map<String, Tle> convert(String body) {
		Map<String, Tle> result = new HashMap<>();
		String[] lines = body.split("\n");
		for (int i = 0; i < lines.length; i += 3) {
			result.put(lines[i], new Tle(new String[] { lines[i], lines[i + 1], lines[i + 2] }));
		}
		return result;
	}

	@Before
	public void start() throws Exception {
		server = new CelestrakServer();
		server.start();
	}

	@After
	public void stop() throws Exception {
		server.stop();
	}

}
