package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ru.r2cloud.Util;
import ru.r2cloud.model.TLE;

public class CelestrakClientTest {

	private HttpServer server;

	@Test
	public void testSuccess() {
		String expectedBody = Util.loadExpected("sample-tle.txt");
		Map<String, TLE> expected = convert(expectedBody);
		server.createContext("/NORAD/elements/active.txt", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.sendResponseHeaders(200, expectedBody.length());
				OutputStream os = exchange.getResponseBody();
				os.write(expectedBody.getBytes(StandardCharsets.UTF_8));
				os.close();
			}
		});

		// one slash is important here
		CelestrakClient client = new CelestrakClient("http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort());
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
		server = HttpServer.create(new InetSocketAddress("localhost", 8000), 0);
		server.start();
	}

	@After
	public void stop() throws Exception {
		server.stop(0);
	}

}
