package ru.r2cloud.lora.r2lora;

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
import java.util.Arrays;
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
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class R2loraClient {

	private static final String MALFORMED_JSON = "MALFORMED_JSON";

	private static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";

	private static final Logger LOG = LoggerFactory.getLogger(LeoSatDataClient.class);

	private HttpClient httpclient;
	private final String hostname;
	private final String basicAuth;

	public R2loraClient(String host, int port, String username, String password, int timeout) {
		this.hostname = "http://" + host + ":" + port;
		this.basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(timeout)).build();
	}

	public LoraStatus getStatus() {
		HttpRequest request = createRequest("/status").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to get r2lora status. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new LoraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
			}
			return readStatus(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to get r2lora status from: " + hostname, e);
			return new LoraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public LoraResponse startObservation(LoraObservationRequest req) {
		LoraResponse result = startObservationInternal(req);
		if (result.getStatus().equals(ResponseStatus.RECEIVING)) {
			LOG.info("r2lora is already receiving. stopping previous and starting again");
			LoraResponse response = stopObservation();
			if (response.getFrames() != null && response.getFrames().size() > 0) {
				for (LoraFrame cur : response.getFrames()) {
					LOG.info("previous unknown observation got some data. Logging it here for manual recovery: {}", Arrays.toString(cur.getData()));
				}
			}
			result = startObservation(req);
		}
		return result;
	}

	private LoraResponse startObservationInternal(LoraObservationRequest req) {
		HttpRequest request = createRequest("/lora/rx/start").header("Content-Type", "application/json").POST(BodyPublishers.ofString(toJson(req))).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to start observation. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new LoraResponse(CONNECTION_FAILURE);
			}
			return readResponse(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to start observation", e);
			return new LoraResponse(CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	public LoraResponse stopObservation() {
		HttpRequest request;
		request = createRequest("/rx/stop").POST(BodyPublishers.noBody()).build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (LOG.isErrorEnabled()) {
					LOG.error("unable to stop observation. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new LoraResponse(CONNECTION_FAILURE);
			}
			return readResponse(response.body());
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to stop observation", e);
			return new LoraResponse(CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static LoraResponse readResponse(String con) {
		JsonValue json;
		try {
			json = Json.parse(con);
		} catch (ParseException e) {
			LOG.info(MALFORMED_JSON);
			return new LoraResponse(MALFORMED_JSON);
		}
		if (!json.isObject()) {
			LOG.info(MALFORMED_JSON);
			return new LoraResponse(MALFORMED_JSON);
		}
		JsonObject obj = json.asObject();
		LoraResponse result = new LoraResponse();
		try {
			result.setStatus(ResponseStatus.valueOf(obj.getString("status", "FAILURE")));
		} catch (Exception e) {
			result.setStatus(ResponseStatus.UNKNOWN);
		}
		JsonValue failureMessage = obj.get("failureMessage");
		if (failureMessage != null) {
			result.setFailureMessage(failureMessage.asString());
		}
		JsonValue frames = obj.get("frames");
		if (frames != null && frames.isArray()) {
			JsonArray framesArray = frames.asArray();
			List<LoraFrame> loraFrames = new ArrayList<>(framesArray.size());
			for (int i = 0; i < framesArray.size(); i++) {
				LoraFrame cur = readFrame(framesArray.get(i));
				if (cur == null) {
					continue;
				}
				loraFrames.add(cur);
			}
			result.setFrames(loraFrames);
		}
		return result;
	}

	private static LoraFrame readFrame(JsonValue val) {
		if (!val.isObject()) {
			return null;
		}
		JsonObject obj = val.asObject();
		LoraFrame result = new LoraFrame();
		result.setData(Util.hexStringToByteArray(obj.getString("data", null)));
		result.setFrequencyError((long) obj.getFloat("frequencyError", 0));
		result.setRssi((short) obj.getInt("rssi", 0));
		result.setSnr(obj.getFloat("snr", 0));
		result.setTimestamp(obj.getLong("timestamp", 0));
		return result;
	}

	private static LoraStatus readStatus(String con) {
		JsonValue json;
		try {
			json = Json.parse(con);
		} catch (ParseException e) {
			LOG.info(MALFORMED_JSON);
			return new LoraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		if (!json.isObject()) {
			LOG.info(MALFORMED_JSON);
			return new LoraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		JsonObject obj = json.asObject();
		LoraStatus result = new LoraStatus();
		result.setStatus(obj.getString("status", null));
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

	private static String toJson(LoraObservationRequest req) {
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
		result.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		result.header("Authorization", basicAuth);
		return result;
	}

}
