package ru.r2cloud.tle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.model.TLE;

public class CelestrakClientTest {

	private final LocalTestServer server = new LocalTestServer(null, null);
	
	@Test
	public void testSuccess() {
		StringBuilder expectedStr = new StringBuilder();
		Map<String, TLE> expected;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(CelestrakClientTest.class.getClassLoader().getResourceAsStream("sample-tle.txt")))) {
			String curLine = null;
			List<String> lines = new ArrayList<>();
			while( (curLine = r.readLine()) != null ) {
				lines.add(curLine);
				expectedStr.append(curLine).append("\n");
			}
			expected = convert(lines);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		server.register("/*", new HttpRequestHandler() {

			@Override
			public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
				response.setEntity(new StringEntity(expectedStr.toString(), StandardCharsets.UTF_8));
			}
		});
		// one slash is important here
		CelestrakClient client = new CelestrakClient("http:/" + server.getServiceAddress());
		Map<String, TLE> actual = client.getWeatherTLE();
		assertEquals(expected.size(), actual.size());
		assertTrue(expected.equals(actual));
	}

	private static Map<String, TLE> convert(List<String> lines) {
		Map<String, TLE> result = new HashMap<String, TLE>();
		for (int i = 0; i < lines.size(); i += 3) {
			result.put(lines.get(i), new TLE(new String[] { lines.get(i), lines.get(i + 1), lines.get(i + 2) }));
		}
		return result;
	}

	@Before
	public void start() throws Exception {
		server.start();
	}

	@After
	public void stop() throws Exception {
		server.stop();
	}

}
