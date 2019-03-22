package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.CelestrakServer;
import ru.r2cloud.Util;
import ru.r2cloud.model.TLE;

public class CelestrakClientTest {

	private CelestrakServer server;

	@Test
	public void testSuccess() {
		String expectedBody = Util.loadExpected("sample-tle.txt");
		Map<String, TLE> expected = convert(expectedBody);
		server.mockResponse(expectedBody);

		// one slash is important here
		CelestrakClient client = new CelestrakClient(server.getUrl());
		Map<String, TLE> actual = client.getTleForActiveSatellites();
		assertEquals(expected.size(), actual.size());
		assertTrue(expected.equals(actual));
	}

	private static Map<String, TLE> convert(String body) {
		Map<String, TLE> result = new HashMap<String, TLE>();
		String[] lines = body.split("\n");
		for (int i = 0; i < lines.length; i += 3) {
			result.put(lines[i], new TLE(new String[] { lines[i], lines[i + 1], lines[i + 2] }));
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
