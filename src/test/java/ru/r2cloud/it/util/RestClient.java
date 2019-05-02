package ru.r2cloud.it.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.crypto.RuntimeCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class RestClient {

	private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

	private HttpClient httpclient;
	private final String baseUrl;
	private String accessToken;

	public RestClient(String baseUrl) throws Exception {
		if (baseUrl == null) {
			this.baseUrl = "http://localhost:8097";
		} else {
			this.baseUrl = baseUrl;
		}
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMinutes(1L)).build();
	}

	public boolean healthy() {
		HttpRequest request = createDefaultRequest("/api/v1/health").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			boolean result = response.statusCode() == 200;
			if (!result) {
				LOG.info("status code: {}", response.statusCode());
			}
			return result;
		} catch (IOException e) {
			LOG.error("unable to get status", e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public void login(String username, String password) {
		HttpResponse<String> response = loginWithResponse(username, password);
		if (response.statusCode() != 200) {
			throw new RuntimeException("unable to login");
		}
	}

	public HttpResponse<String> loginWithResponse(String username, String password) {
		JsonObject json = Json.object();
		json.add("username", username);
		json.add("password", password);
		HttpRequest request = createDefaultRequest("/api/v1/accessToken").header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString())).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return response;
			}
			JsonObject object = (JsonObject) Json.parse(response.body());
			accessToken = object.get("access_token").asString();
			return response;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeCryptoException();
		}
	}

	public void setup(String keyword, String username, String password) {
		HttpResponse<String> response = setupWithResponse(keyword, username, password);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public HttpResponse<String> setupWithResponse(String keyword, String username, String password) {
		LOG.info("setup: {}", username);
		JsonObject json = Json.object();
		json.add("keyword", keyword);
		json.add("username", username);
		json.add("password", password);
		HttpRequest request = createDefaultRequest("/api/v1/setup/setup").header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString())).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	private JsonObject getData(String url) {
		HttpRequest request = createAuthRequest(url).GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.info("response: {}", response.body());
				throw new RuntimeException("invalid status code: " + response.statusCode());
			}
			return (JsonObject) Json.parse(response.body());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	private JsonArray getDataArray(String url) {
		HttpRequest request = createAuthRequest(url).GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.info("response: {}", response.body());
				throw new RuntimeException("invalid status code: " + response.statusCode());
			}
			return (JsonArray) Json.parse(response.body());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public HttpResponse<String> saveR2CloudConfigurationWithResponse(String apiKey, boolean syncSpectogram) {
		LOG.info("save r2cloud configuration");
		JsonObject json = Json.object();
		json.add("apiKey", apiKey);
		json.add("syncSpectogram", syncSpectogram);
		HttpRequest request = createJsonPost("/api/v1/admin/config/r2cloud", json).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	public void saveR2CloudConfiguration(String apiKey, boolean syncSpectogram) {
		HttpResponse<String> response = saveR2CloudConfigurationWithResponse(apiKey, syncSpectogram);
		if (response.statusCode() != 200) {
			LOG.info("status code: {}", response.statusCode());
		}
	}

	public JsonObject getR2CloudConfiguration() {
		LOG.info("get r2cloud configuration");
		return getData("/api/v1/admin/config/r2cloud");
	}

	public JsonArray getMetrics() {
		return getDataArray("/api/v1/admin/status/metrics");
	}

	private HttpRequest.Builder createJsonPost(String path, JsonObject obj) {
		return createAuthRequest(path).header("Content-Type", "application/json").POST(BodyPublishers.ofString(obj.toString(), StandardCharsets.UTF_8));
	}

	private HttpRequest.Builder createAuthRequest(String path) {
		return createDefaultRequest(path).header("Authorization", "Bearer " + accessToken);
	}

	private HttpRequest.Builder createDefaultRequest(String path) {
		return HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).timeout(Duration.ofMinutes(1L)).header("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
	}

	public HttpResponse<String> resetPasswordWithResponse(String username) {
		LOG.info("setup: {}", username);
		JsonObject json = Json.object();
		json.add("username", username);
		HttpRequest request = createDefaultRequest("/api/v1/setup/restore").header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString())).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public void resetPassword(String username) {
		HttpResponse<String> response = resetPasswordWithResponse(username);
		if (response.statusCode() != 200) {
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public JsonObject getConfigured() {
		return getData("/api/v1/configured");
	}

	public JsonObject getGeneralConfiguration() {
		return getData("/api/v1/admin/config/general");
	}

	public JsonObject getTle() {
		return getData("/api/v1/admin/tle");
	}

	public JsonObject getOverview() {
		return getData("/api/v1/admin/status/overview");
	}

	public void setGeneralConfiguration(Double lat, Double lng, boolean autoUpdate) {
		HttpResponse<String> response = setGeneralConfigurationWithResponse(lat, lng, autoUpdate);
		if (response.statusCode() != 200) {
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public HttpResponse<String> setGeneralConfigurationWithResponse(Double lat, Double lng, boolean autoUpdate) {
		JsonObject json = Json.object();
		if (lat != null) {
			json.add("lat", lat);
		}
		if (lng != null) {
			json.add("lng", lng);
		}
		json.add("autoUpdate", autoUpdate);
		HttpRequest request = createJsonPost("/api/v1/admin/config/general", json).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public JsonArray getSchedule() {
		HttpRequest request = createAuthRequest("/api/v1/admin/schedule/list").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.info("response: {}", response.body());
				throw new RuntimeException("invalid status code: " + response.statusCode());
			}
			return (JsonArray) Json.parse(response.body());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public HttpResponse<String> updateScheduleWithResponse(String id, boolean enabled) {
		JsonObject entity = new JsonObject();
		if (id != null) {
			entity.add("id", id);
		}
		entity.add("enabled", enabled);
		HttpRequest request = createJsonPost("/api/v1/admin/schedule/save", entity).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public JsonObject updateSchedule(String id, boolean enabled) {
		HttpResponse<String> response = updateScheduleWithResponse(id, enabled);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
		return (JsonObject) Json.parse(response.body());
	}

	public HttpResponse<String> getFileResponse(String url) {
		HttpRequest request = createAuthRequest(url).GET().build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public String getFile(String url) {
		HttpResponse<String> response = getFileResponse(url);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
		return response.body();
	}

	public HttpResponse<Path> downloadFile(String url, Path file) {
		return downloadFile(url, file, (String[]) null);
	}

	public HttpResponse<Path> downloadFile(String url, Path file, String... headers) {
		Builder req = createAuthRequest(url);
		if (headers != null) {
			for (int i = 0; i < headers.length; i += 2) {
				req.header(headers[i], headers[i + 1]);
			}
		}
		HttpRequest request = req.GET().build();
		try {
			return httpclient.send(request, BodyHandlers.ofFile(file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public String scheduleStart(String satelliteId) {
		JsonObject entity = new JsonObject();
		if (satelliteId != null) {
			entity.add("id", satelliteId);
		}
		HttpRequest request = createJsonPost("/api/v1/admin/schedule/immediately/start", entity).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.info("response: {}", response.body());
				throw new RuntimeException("invalid status code: " + response.statusCode());
			}
			JsonObject json = (JsonObject) Json.parse(response.body());
			return json.getString("id", null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public void scheduleComplete(String satelliteId) {
		JsonObject entity = new JsonObject();
		if (satelliteId != null) {
			entity.add("id", satelliteId);
		}
		HttpRequest request = createJsonPost("/api/v1/admin/schedule/immediately/complete", entity).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOG.info("response: {}", response.body());
				throw new RuntimeException("invalid status code: " + response.statusCode());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public HttpResponse<String> getObservationResponse(String satelliteId, String observationId) {
		Map<String, String> params = new HashMap<String, String>();
		if (satelliteId != null) {
			params.put("satelliteId", satelliteId);
		}
		if (observationId != null) {
			params.put("id", observationId);
		}
		HttpRequest request = createAuthRequest("/api/v1/admin/observation/load" + createQuery(params)).GET().build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public JsonObject getObservation(String satelliteId, String observationId) {
		HttpResponse<String> response = getObservationResponse(satelliteId, observationId);
		if (response.statusCode() == 404) {
			return null;
		}
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
		return (JsonObject) Json.parse(response.body());
	}

	private static String createQuery(Map<String, String> params) {
		StringBuilder result = new StringBuilder();
		if (params == null || params.isEmpty()) {
			return result.toString();
		}
		result.append("?");
		boolean first = true;
		for (Entry<String, String> cur : params.entrySet()) {
			if (!first) {
				result.append("&");
			}
			result.append(cur.getKey()).append('=').append(URLEncoder.encode(cur.getValue(), StandardCharsets.UTF_8));
			first = false;
		}
		return result.toString();
	}

}
