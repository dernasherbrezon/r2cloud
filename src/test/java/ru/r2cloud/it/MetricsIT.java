package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.core.RrdBackendFactory;
import com.aerse.core.RrdDb;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.it.util.RegisteredTest;

public class MetricsIT extends RegisteredTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testLoadMetrics() {
		JsonArray metrics = client.getMetrics();
		// there might be more metrics. these 2 guaranteed to exist
		assertMetric(metrics, "heap", "BYTES");
		assertMetric(metrics, "ppm", "NORMAL");
	}

	private void assertMetric(JsonArray metrics, String name, String expectedFormat) {
		JsonObject metric = getById(metrics, name);
		assertNotNull(metric);
		String url = metric.getString("url", null);
		assertNotNull(url);
		assertEquals(expectedFormat, metric.getString("format", null));

		HttpResponse<Path> response = client.downloadFile(url, Paths.get(tempFolder.getRoot().getAbsolutePath(), UUID.randomUUID().toString()));
		assertEquals(200, response.statusCode());
		assertEquals("application/octet-stream", response.headers().firstValue("content-type").get());

		try (RrdDb rrd = new RrdDb(response.body().toString(), RrdBackendFactory.getFactory("FILE"))) {
			assertEquals(1, rrd.getDsCount());
			assertEquals(3, rrd.getArcCount());
		} catch (IOException e) {
			fail("unable to read: " + e.getMessage());
		}
	}

	private static JsonObject getById(JsonArray metrics, String id) {
		for (int i = 0; i < metrics.size(); i++) {
			JsonObject cur = (JsonObject) metrics.get(i);
			if (cur.getString("id", "").equals(id)) {
				return cur;
			}
		}
		return null;
	}

}
