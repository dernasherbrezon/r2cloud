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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class LeoSatDataClient {

	private static final String OBSERVATION_BASEPATH = "/api/v1/observation";
	private static final int MAX_RETRIES = 3;
	private static final long GUARANTEED_PERIOD = 1000L;
	private static final Logger LOG = LoggerFactory.getLogger(LeoSatDataClient.class);

	private final HttpClient httpclient;
	private final String hostname;
	private final Configuration config;
	private final Duration timeout;
	private final Clock clock;

	public LeoSatDataClient(Configuration config, Clock clock) {
		this.config = config;
		this.clock = clock;
		this.hostname = config.getProperty("leosatdata.hostname");
		this.timeout = Duration.ofMillis(config.getInteger("leosatdata.connectionTimeout"));
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(timeout).build();
	}

	public Long saveMeta(Observation observation) {
		if (observation == null) {
			return null;
		}
		HttpRequest request = createJsonRequest(OBSERVATION_BASEPATH, observation.toJson(null)).build();
		try {
			HttpResponse<String> response = sendWithRetry(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to save meta. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return null;
			}
			return readObservationId(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "[" + observation.getId() + "] unable to save meta", e);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public List<Satellite> loadNewLaunches() {
		HttpRequest request = createRequest("/api/v1/satellite/newlaunch2").GET().build();
		try {
			HttpResponse<String> response = sendWithRetry(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to load new launches. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return Collections.emptyList();
			}
			return readNewLaunches(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load new launches", e);
			return Collections.emptyList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public List<Satellite> loadSatellites() {
		HttpRequest request = createRequest("/api/v1/satellite").GET().build();
		try {
			HttpResponse<String> response = sendWithRetry(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to load satellites. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return Collections.emptyList();
			}
			return readSatellites(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to load satellites from leosatdata", e);
			return Collections.emptyList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public void saveJpeg(Long id, File getaPath) {
		upload(OBSERVATION_BASEPATH + "/" + id + "/data", getaPath, "image/jpeg");
	}

	public void saveBinary(Long id, File getaPath) {
		upload(OBSERVATION_BASEPATH + "/" + id + "/data", getaPath, "application/octet-stream");
	}

	public void saveSpectogram(Long id, File spectogramPath) {
		upload(OBSERVATION_BASEPATH + "/" + id + "/spectogram", spectogramPath, "image/png");
	}

	private void upload(String url, File file, String contentType) {
		HttpRequest request;
		try {
			request = createRequest(url).header("Content-Type", contentType).PUT(BodyPublishers.ofFile(file.toPath())).build();
		} catch (FileNotFoundException e) {
			LOG.error("unable to upload: {}", url, e);
			return;
		}
		httpclient.sendAsync(request, BodyHandlers.ofString()).handleAsync((r, t) -> uploadWithRetry(request, BodyHandlers.ofString(), r, t, 1)).thenCompose(Function.identity());
	}

	private <T> CompletableFuture<HttpResponse<T>> uploadWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse<T> response, Throwable t, int currentRetry) {
		if (currentRetry > MAX_RETRIES || (response != null && response.statusCode() < 500)) {
			if (t != null) {
				Util.logIOException(LOG, "unable to upload: ", t);
				return CompletableFuture.failedFuture(t);
			}
			return CompletableFuture.completedFuture(response);
		}
		if (t != null) {
			LOG.info("unable to upload: {}. retry {}", t.getMessage(), currentRetry);
		} else if (response != null) {
			LOG.info("unable to upload. status code: {}. retry {}", response.statusCode(), currentRetry);
		}
		// linear backoff with random jitter
		try {
			Thread.sleep(GUARANTEED_PERIOD * currentRetry + (long) (Math.random() * GUARANTEED_PERIOD * currentRetry));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return CompletableFuture.failedFuture(e);
		}
		return httpclient.sendAsync(request, handler).handleAsync((r2, t2) -> uploadWithRetry(request, handler, r2, t2, currentRetry + 1)).thenCompose(Function.identity());
	}

	private HttpResponse<String> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) throws IOException, InterruptedException {
		int currentRetry = 0;
		while (true) {
			try {
				HttpResponse<String> result = httpclient.send(request, responseBodyHandler);
				// retry for any server-side errors
				// can happen during server upgrade
				if (result.statusCode() < 500) {
					return result;
				}
				currentRetry++;
				if (currentRetry > MAX_RETRIES) {
					return result;
				}
				LOG.info("unable to send. status code: {}. retry {}", result.statusCode(), currentRetry);
			} catch (IOException e) {
				currentRetry++;
				if (currentRetry > MAX_RETRIES) {
					throw e;
				}
				Util.logIOException(LOG, false, "unable to send. retry " + currentRetry, e);
			}
			// linear backoff with random jitter
			Thread.sleep(GUARANTEED_PERIOD * currentRetry + (long) (Math.random() * GUARANTEED_PERIOD * currentRetry));
		}
	}

	private static List<Satellite> readSatellites(String body) {
		JsonValue parsedJson;
		try {
			parsedJson = Json.parse(body);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		if (!parsedJson.isArray()) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		JsonArray parsedArray = parsedJson.asArray();
		List<Satellite> result = new ArrayList<>();
		for (int i = 0; i < parsedArray.size(); i++) {
			JsonValue jsonValue = parsedArray.get(i);
			if (!jsonValue.isObject()) {
				continue;
			}
			JsonObject asObject = jsonValue.asObject();
			result.add(Satellite.fromJson(asObject));
		}
		return result;
	}

	private List<Satellite> readNewLaunches(String body) {
		JsonValue parsedJson;
		try {
			parsedJson = Json.parse(body);
		} catch (ParseException e) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		if (!parsedJson.isArray()) {
			LOG.info("malformed json");
			return Collections.emptyList();
		}
		JsonArray parsedArray = parsedJson.asArray();
		List<Satellite> result = new ArrayList<>();
		for (int i = 0; i < parsedArray.size(); i++) {
			JsonValue jsonValue = parsedArray.get(i);
			if (!jsonValue.isObject()) {
				continue;
			}
			JsonObject asObject = jsonValue.asObject();
			Satellite newLaunch = Satellite.fromJson(asObject);
			if (newLaunch == null || newLaunch.getName() == null) {
				continue;
			}
			// new launch doesn't have noradId
			newLaunch.setId(asObject.getString("id", null));
			if (newLaunch.getId() == null) {
				continue;
			}
			// override priority and TLE
			newLaunch.setPriority(Priority.HIGH);
			newLaunch.setTle(readTle(asObject.get("tle")));
			result.add(newLaunch);
		}
		return result;
	}

	private Tle readTle(JsonValue tle) {
		if (tle == null || !tle.isObject()) {
			return null;
		}
		JsonObject tleObj = tle.asObject();

		String line1 = tleObj.getString("line1", null);
		if (line1 == null) {
			return null;
		}
		String line2 = tleObj.getString("line2", null);
		if (line2 == null) {
			return null;
		}
		String line3 = tleObj.getString("line3", null);
		if (line3 == null) {
			return null;
		}
		if (!org.orekit.propagation.analytical.tle.TLE.isFormatOK(line2, line3)) {
			LOG.error("invalid tle format");
			return null;
		}
		Tle result = new Tle(new String[] { line1, line2, line3 });
		// assume downloaded TLE is always fresh
		result.setLastUpdateTime(clock.millis());
		return result;
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

	private HttpRequest.Builder createJsonRequest(String path, JsonValue json) {
		return createRequest(path).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8));
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(hostname + path));
		result.timeout(timeout);
		result.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		result.header("Authorization", config.getProperty("r2cloud.apiKey"));
		return result;
	}

}
