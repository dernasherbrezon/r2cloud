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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.model.GeneralConfiguration;

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
			throw new RuntimeException();
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

	public HttpResponse<String> getOptions(String url) {
		HttpRequest request = createDefaultRequest(url).method("OPTIONS", BodyPublishers.noBody()).build();
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

	public HttpResponse<String> saveR2CloudConfigurationWithResponse(String apiKey, boolean syncSpectogram, boolean newLaunch) {
		LOG.info("save r2cloud configuration");
		JsonObject json = Json.object();
		json.add("apiKey", apiKey);
		json.add("syncSpectogram", syncSpectogram);
		json.add("newLaunch", newLaunch);
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

	public HttpResponse<String> getDataWithResponse(String url) {
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

	public void saveR2CloudConfiguration(String apiKey, boolean syncSpectogram, boolean newLaunch) {
		HttpResponse<String> response = saveR2CloudConfigurationWithResponse(apiKey, syncSpectogram, newLaunch);
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

	public JsonObject getSsl() {
		return getData("/api/v1/admin/config/ssl");
	}

	public HttpResponse<String> saveSslConfigurationResponse(Boolean enabled, Boolean agreeWithToC, String domain) {
		JsonObject json = Json.object();
		json.add("enabled", enabled);
		json.add("agreeWithToC", agreeWithToC);
		json.add("domain", domain);
		HttpRequest request = createJsonPost("/api/v1/admin/config/ssl", json).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
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

	public void setGeneralConfiguration(GeneralConfiguration config) {
		HttpResponse<String> response = setGeneralConfigurationWithResponse(config);
		if (response.statusCode() != 200) {
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public HttpResponse<String> setGeneralConfigurationWithResponse(GeneralConfiguration config) {
		JsonObject json = Json.object();
		if (config.getLat() != null) {
			json.add("lat", config.getLat());
		}
		if (config.getLng() != null) {
			json.add("lng", config.getLng());
		}
		json.add("autoUpdate", config.isAutoUpdate());
		if (config.getPpm() != null) {
			json.add("ppm", config.getPpm());
		}
		if (config.getElevationMin() != null) {
			json.add("elevationMin", config.getElevationMin());
		}
		if (config.getElevationGuaranteed() != null) {
			json.add("elevationGuaranteed", config.getElevationGuaranteed());
		}
		json.add("rotationEnabled", config.isRotationEnabled());
		if (config.getRotctrldHostname() != null) {
			json.add("rotctrldHostname", config.getRotctrldHostname());
		}
		if (config.getRotctrldPort() != null) {
			json.add("rotctrldPort", config.getRotctrldPort());
		}
		if (config.getRotatorTolerance() != null) {
			json.add("rotatorTolerance", config.getRotatorTolerance());
		}
		if (config.getRotatorCycle() != null) {
			json.add("rotatorCycle", config.getRotatorCycle());
		}
		if (config.getGain() != null) {
			json.add("gain", config.getGain());
		}
		json.add("biast", config.isBiast());
		json.add("presentationMode", config.isPresentationMode());
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

	public JsonArray getFullSchedule() {
		HttpRequest request = createAuthRequest("/api/v1/admin/schedule/full").GET().build();
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

	public HttpResponse<String> postData(String url, String data) {
		HttpRequest request = createDefaultRequest(url).POST(BodyPublishers.ofString(data, StandardCharsets.UTF_8)).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
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

	public HttpResponse<String> scheduleStartResponse(String satelliteId) {
		JsonObject entity = new JsonObject();
		if (satelliteId != null) {
			entity.add("id", satelliteId);
		}
		HttpRequest request = createJsonPost("/api/v1/admin/schedule/immediately/start", entity).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public List<String> scheduleStart(String satelliteId) {
		HttpResponse<String> response = scheduleStartResponse(satelliteId);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
		JsonObject json = (JsonObject) Json.parse(response.body());
		JsonValue ids = json.get("ids");
		if (ids == null || !ids.isArray()) {
			return Collections.emptyList();
		}
		JsonArray idsArray = ids.asArray();
		List<String> result = new ArrayList<>();
		for (int i = 0; i < idsArray.size(); i++) {
			result.add(idsArray.get(i).asString());
		}
		return result;
	}

	public HttpResponse<String> scheduleCompleteResponse(String observationId) {
		JsonObject entity = new JsonObject();
		if (observationId != null) {
			entity.add("id", observationId);
		}
		HttpRequest request = createJsonPost("/api/v1/admin/schedule/immediately/complete", entity).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public void scheduleComplete(String observationId) {
		HttpResponse<String> response = scheduleCompleteResponse(observationId);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public HttpResponse<String> getObservationResponse(String url, String satelliteId, String observationId) {
		Map<String, String> params = new HashMap<>();
		if (satelliteId != null) {
			params.put("satelliteId", satelliteId);
		}
		if (observationId != null) {
			params.put("id", observationId);
		}
		HttpRequest request = createAuthRequest(url + createQuery(params)).GET().build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

	public HttpResponse<String> getObservationResponse(String satelliteId, String observationId) {
		return getObservationResponse("/api/v1/admin/observation/load", satelliteId, observationId);
	}

	public JsonArray getObservationList() {
		HttpRequest request = createAuthRequest("/api/v1/admin/observation/list").GET().build();
		HttpResponse<String> response;
		try {
			response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
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

	public JsonObject requestObservationSpectogram(String satelliteId, String observationId) {
		HttpResponse<String> response = requestObservationSpectogramResponse(satelliteId, observationId);
		if (response.statusCode() != 200) {
			LOG.info("response: {}", response.body());
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
		return (JsonObject) Json.parse(response.body());
	}

	public HttpResponse<String> requestObservationSpectogramResponse(String satelliteId, String observationId) {
		JsonObject json = Json.object();
		json.add("satelliteId", satelliteId);
		json.add("id", observationId);
		HttpRequest request = createJsonPost("/api/v1/admin/observation/spectogram", json).build();
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

	public JsonObject getObservationPresentation(String satelliteId, String observationId) {
		HttpResponse<String> response = getObservationResponse("/api/v1/observation/load", satelliteId, observationId);
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

	public JsonObject getDdnsConfiguration() {
		return getData("/api/v1/admin/config/ddns");
	}

	public JsonObject getPresentationModeData() {
		return getData("/api/v1/presentationMode");
	}

	public void saveDdnsConfiguration(String type, String username, String password, String domain) {
		HttpResponse<String> response = saveDdnsConfigurationResponse(type, username, password, domain);
		if (response.statusCode() != 200) {
			throw new RuntimeException("invalid status code: " + response.statusCode());
		}
	}

	public HttpResponse<String> saveDdnsConfigurationResponse(String type, String username, String password, String domain) {
		JsonObject json = Json.object();
		if (type != null) {
			json.add("type", type);
		}
		if (username != null) {
			json.add("username", username);
		}
		if (password != null) {
			json.add("password", password);
		}
		if (domain != null) {
			json.add("domain", domain);
		}
		HttpRequest request = createJsonPost("/api/v1/admin/config/ddns", json).build();
		try {
			return httpclient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("unable to send request");
		}
	}

}
