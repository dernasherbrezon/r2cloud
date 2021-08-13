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
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.FrequencySource;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class R2ServerClient {

	private static final String OBSERVATION_BASEPATH = "/api/v1/observation";

	private static final Logger LOG = LoggerFactory.getLogger(R2ServerClient.class);

	private HttpClient httpclient;
	private final String hostname;
	private final Configuration config;

	public R2ServerClient(Configuration config) {
		this.config = config;
		this.hostname = config.getProperty("r2server.hostname");
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(config.getInteger("r2server.connectionTimeout"))).build();
	}

	public Long saveMeta(Observation observation) {
		if (observation == null) {
			return null;
		}
		HttpRequest request = createJsonRequest(OBSERVATION_BASEPATH, observation.toJson(null)).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
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
		HttpRequest request = createRequest("/api/v1/satellite/newlaunch").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
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
		httpclient.sendAsync(request, BodyHandlers.ofString()).exceptionally(ex -> {
			Util.logIOException(LOG, "unable to upload: " + url, ex);
			return null;
		}).thenAccept(response -> {
			if (response != null && response.statusCode() != 200 && LOG.isErrorEnabled()) {
				LOG.error("unable to upload: {} response code: {}. response: {}", url, response.statusCode(), response.body());
			}
		});
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
			Satellite newLaunch = readNewLaunch(jsonValue.asObject());
			if (newLaunch == null) {
				continue;
			}
			result.add(newLaunch);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Satellite readNewLaunch(JsonObject json) {
		Satellite result = new Satellite();
		String id = json.getString("id", null);
		if (id == null) {
			return null;
		}
		result.setId(id);
		String name = json.getString("name", null);
		if (name == null) {
			return null;
		}
		result.setName(name);
		long frequency = json.getLong("frequency", 0);
		if (frequency == 0) {
			return null;
		}
		result.setFrequency(frequency);

		String modulation = json.getString("modulation", null);
		if (modulation == null) {
			return null;
		}
		try {
			result.setModulation(Modulation.valueOf(modulation));
		} catch (Exception e) {
			return null;
		}
		String framing = json.getString("framing", null);
		if (framing == null) {
			return null;
		}
		try {
			result.setFraming(Framing.valueOf(framing));
		} catch (Exception e) {
			return null;
		}
		result.setSource(FrequencySource.TELEMETRY);
		result.setPriority(Priority.HIGH);
		// by default enabled, but can be overriden by user from UI
		String enabledStr = config.getProperty("satellites." + result.getId() + ".enabled");
		if (enabledStr != null) {
			result.setEnabled(Boolean.valueOf(enabledStr));
		} else {
			result.setEnabled(true);
		}
		long bandwidth = json.getLong("bandwidth", 0);
		if (bandwidth == 0) {
			return null;
		}
		result.setBandwidth(bandwidth);
		JsonValue jsonRates = json.get("baudRates");
		if (jsonRates != null && jsonRates.isArray()) {
			result.setBaudRates(convertToIntegerList(jsonRates.asArray()));
		}
		if (result.getBaudRates() == null || result.getBaudRates().isEmpty()) {
			return null;
		}
		String beaconClassStr = json.getString("beaconClass", null);
		if (beaconClassStr == null) {
			return null;
		}
		try {
			result.setBeaconClass((Class<? extends Beacon>) Class.forName(beaconClassStr));
		} catch (ClassNotFoundException e) {
			return null;
		}
		result.setBeaconSizeBytes(json.getInt("beaconSizeBytes", 0));

		Tle tle = readTle(json.get("tle"));
		if (tle == null) {
			return null;
		}
		long startTimeMillis = json.getLong("start", 0);
		if (startTimeMillis != 0) {
			result.setStart(new Date(startTimeMillis));
		}
		long endTimeMillis = json.getLong("end", 0);
		if (endTimeMillis != 0) {
			result.setEnd(new Date(endTimeMillis));
		}

		return result;
	}

	private static Tle readTle(JsonValue tle) {
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
			return null;
		}
		return new Tle(new String[] { line1, line2, line3 });
	}

	private static List<Integer> convertToIntegerList(JsonArray array) {
		List<Integer> result = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			result.add(array.get(i).asInt());
		}
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

	public void saveMetrics(JsonArray o) {
		if (o == null || o.size() == 0) {
			return;
		}
		HttpRequest request = createJsonRequest("/api/v1/metrics", o).build();
		httpclient.sendAsync(request, BodyHandlers.ofString()).exceptionally(ex -> {
			Util.logIOException(LOG, "unable to save metrics", ex);
			return null;
		}).thenAccept(response -> {
			if (response != null && response.statusCode() != 200 && LOG.isErrorEnabled()) {
				LOG.error("unable to save metrics. response code: {}. response: {}", response.statusCode(), response.body());
			}
		});
	}

	private HttpRequest.Builder createJsonRequest(String path, JsonValue json) {
		return createRequest(path).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8));
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(hostname + path));
		result.timeout(Duration.ofMinutes(1L));
		result.header("User-Agent", R2Cloud.getVersion() + " info@r2cloud.ru");
		result.header("Authorization", config.getProperty("r2cloud.apiKey"));
		return result;
	}

}
