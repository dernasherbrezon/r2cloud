package ru.r2cloud.cloud;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.util.Configuration;

public class R2CloudClient {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudClient.class);

	private HttpClient httpclient;
	private final String hostname;
	private final String apiKey;

	public R2CloudClient(Configuration config) {
		this.hostname = config.getProperty("r2cloud.hostname");
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(config.getInteger("r2cloud.connectionTimeout"))).build();
		this.apiKey = config.getProperty("r2cloud.apiKey");
	}

	public Long saveMeta(ObservationFull observation) {
		if (observation == null) {
			return null;
		}
		HttpRequest request = createJsonRequest("/api/v1/observation", observation.toJson()).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.error("unable to save meta. response code: {}. See logs for details", response.statusCode());
				LOG.info(response.body());
				return null;
			}
			return readObservationId(response.body());
		} catch (IOException e) {
			LOG.error("unable to save meta", e);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	public void saveJpeg(Long id, File getaPath) {
		upload("/api/v1/observation/" + id + "/data", getaPath, "image/jpeg");
	}

	public void saveBinary(Long id, File getaPath) {
		upload("/api/v1/observation/" + id + "/data", getaPath, "application/octet-stream");
	}

	public void saveSpectogram(Long id, File spectogramPath) {
		upload("/api/v1/observation/" + id + "/spectogram", spectogramPath, "image/png");
	}

	private void upload(String url, File file, String contentType) {
		HttpRequest request;
		try {
			request = createRequest(url).header("Content-Type", contentType).PUT(BodyPublishers.ofFile(file.toPath())).build();
		} catch (FileNotFoundException e) {
			LOG.error("unable to upload", e);
			return;
		}
		httpclient.sendAsync(request, BodyHandlers.ofString()).thenAccept(response -> {
			if (response.statusCode() != 200) {
				LOG.error("unable to upload. response code: {}. See logs for details", response.statusCode());
				LOG.info(response.body());
			}
		});
	}

	private static Long readObservationId(String con) {
		JsonValue result;
		try {
			result = Json.parse(con);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return null;
		}
		if (!result.isObject()) {
			LOG.info("malformed json");
			return null;
		}
		JsonObject resultObj = result.asObject();
		String status = resultObj.getString("status", null);
		if (status == null || !status.equalsIgnoreCase("SUCCESS")) {
			LOG.info("response error: {}", resultObj);
			return null;
		}
		long id = resultObj.getLong("id", -1);
		if (id == -1) {
			return null;
		}
		return id;
	}

	public void saveMetrics(JsonArray o) {
		if (o == null || o.size() == 0) {
			return;
		}
		HttpRequest request = createJsonRequest("/api/v1/metrics", o).build();
		httpclient.sendAsync(request, BodyHandlers.ofString()).thenAccept(response -> {
			if (response.statusCode() != 200) {
				LOG.error("unable to save meta. response code: {}. See logs for details", response.statusCode());
				LOG.info(response.body());
			}
		});
	}

	private HttpRequest.Builder createJsonRequest(String path, JsonValue json) {
		return createRequest(path).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8));
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(hostname + path));
		result.timeout(Duration.ofMinutes(1L));
		result.header("User-Agent", "r2cloud/0.2 info@r2cloud.ru");
		result.header("Authorization", apiKey);
		return result;
	}

}
