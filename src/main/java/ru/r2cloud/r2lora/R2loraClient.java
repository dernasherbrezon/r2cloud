package ru.r2cloud.r2lora;

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
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.cloud.LeoSatDataClient;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class R2loraClient {

	private static final String MALFORMED_JSON = "MALFORMED_JSON";

	private static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";

	private static final Logger LOG = LoggerFactory.getLogger(LeoSatDataClient.class);

	private HttpClient httpclient;
	private final String hostname;
	private final String basicAuth;

	public R2loraClient(String hostport, String username, String password, int timeout) {
		this.hostname = "http://" + hostport;
		this.basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(timeout)).build();
	}

	public R2loraStatus getStatus() {
		HttpRequest request = createRequest("/status").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to get r2lora status. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new R2loraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
			}
			return readStatus(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to get r2lora status from: " + hostname, e);
			return new R2loraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public R2loraResponse startObservation(R2loraObservationRequest req) {
		HttpRequest request;
		request = createRequest("/lora/rx/start").header("Content-Type", "application/json").POST(BodyPublishers.ofString(toJson(req))).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to start observation. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new R2loraResponse(CONNECTION_FAILURE);
			}
			return readResponse(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to start observation", e);
			return new R2loraResponse(CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public R2loraResponse stopObservation() {
		HttpRequest request;
		request = createRequest("/rx/stop").POST(BodyPublishers.noBody()).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to stop observation. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new R2loraResponse(CONNECTION_FAILURE);
			}
			return readResponse(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to stop observation", e);
			return new R2loraResponse(CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static R2loraResponse readResponse(String con) {
		JsonValue json;
		try {
			json = Json.parse(con);
		} catch (ParseException e) {
			LOG.info(MALFORMED_JSON);
			return new R2loraResponse(MALFORMED_JSON);
		}
		if (!json.isObject()) {
			LOG.info(MALFORMED_JSON);
			return new R2loraResponse(MALFORMED_JSON);
		}
		JsonObject obj = json.asObject();
		R2loraResponse result = new R2loraResponse();
		result.setStatus(ResponseStatus.valueOf(obj.getString("status", "FAILURE")));
		JsonValue failureMessage = obj.get("failureMessage");
		if (failureMessage != null) {
			result.setFailureMessage(failureMessage.asString());
		}
		JsonValue frames = obj.get("frames");
		if (frames != null && frames.isArray()) {
			JsonArray framesArray = frames.asArray();
			List<R2loraFrame> loraFrames = new ArrayList<>(framesArray.size());
			for (int i = 0; i < framesArray.size(); i++) {
				R2loraFrame cur = readFrame(framesArray.get(i));
				if (cur == null) {
					continue;
				}
				loraFrames.add(cur);
			}
			result.setFrames(loraFrames);
		}
		return result;
	}

	private static R2loraFrame readFrame(JsonValue val) {
		if (!val.isObject()) {
			return null;
		}
		JsonObject obj = val.asObject();
		R2loraFrame result = new R2loraFrame();
		result.setData(Util.hexStringToByteArray(obj.getString("data", null)));
		result.setFrequencyError(obj.getFloat("frequencyError", 0));
		result.setRssi(obj.getFloat("rssi", 0));
		result.setSnr(obj.getFloat("snr", 0));
		result.setTimestamp(obj.getLong("timestamp", 0));
		return result;
	}

	private static R2loraStatus readStatus(String con) {
		JsonValue json;
		try {
			json = Json.parse(con);
		} catch (ParseException e) {
			LOG.info(MALFORMED_JSON);
			return new R2loraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		if (!json.isObject()) {
			LOG.info(MALFORMED_JSON);
			return new R2loraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		JsonObject obj = json.asObject();
		R2loraStatus result = new R2loraStatus();
		result.setStatus(obj.getString("status", null));
		result.setChipTemperature(obj.getInt("chipTemperature", 0));
		result.setDeviceStatus(DeviceConnectionStatus.CONNECTED);

		List<ModulationConfig> configs = new ArrayList<>();
		// interested only in lora parameters
		ModulationConfig loraConfig = readConfig(obj, "lora");
		if (loraConfig != null) {
			configs.add(loraConfig);
		}
		result.setConfigs(configs);
		return result;
	}

	private static ModulationConfig readConfig(JsonObject obj, String name) {
		JsonValue value = obj.get(name);
		if (value == null || !value.isObject()) {
			return null;
		}
		JsonObject modulationObj = value.asObject();
		ModulationConfig result = new ModulationConfig();
		result.setName(name);
		result.setMaxFrequency(modulationObj.getFloat("maxFreq", 0));
		result.setMinFrequency(modulationObj.getFloat("minFreq", 0));
		return result;
	}

	private static String toJson(R2loraObservationRequest req) {
		JsonObject json = new JsonObject();
		json.add("freq", req.getFrequency());
		json.add("bw", req.getBw());
		json.add("sf", req.getSf());
		json.add("cr", req.getCr());
		json.add("syncWord", req.getSyncword());
		json.add("preambleLength", req.getPreambleLength());
		json.add("gain", req.getGain());
		json.add("ldro", req.getLdro());
		return json.toString();
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(hostname + path));
		result.timeout(Duration.ofMinutes(1L));
		result.header("User-Agent", R2Cloud.getVersion() + " info@r2cloud.ru");
		result.header("Authorization", basicAuth);
		return result;
	}

}
