package ru.r2cloud.lora.loraat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
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
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;
import ru.r2cloud.util.Util;

public class LoraAtWifiClient implements LoraAtClient {

	private static final String MALFORMED_JSON = "MALFORMED_JSON";
	private static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";
	private final static Logger LOG = LoggerFactory.getLogger(LoraAtWifiClient.class);

	private HttpClient httpclient;
	private final String hostname;
	private final String basicAuth;

	public LoraAtWifiClient(String host, int port, String username, String password, int timeout) {
		this.hostname = "http://" + host + ":" + port;
		this.basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(timeout)).build();
	}

	@Override
	public LoraStatus getStatus() {
		HttpRequest request = createRequest("/api/v2/status").GET().build();
		try {
			HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				if (response.statusCode() != 404 && LOG.isErrorEnabled()) {
					LOG.error("unable to get status. response code: {}. response: {}", response.statusCode(), response.body());
				}
				return new LoraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
			}
			JsonObject status = Json.parse(response.body()).asObject();
			ModulationConfig loraConfig = new ModulationConfig();
			loraConfig.setName("lora");
			loraConfig.setMinFrequency(status.getLong("minFreq", 0));
			loraConfig.setMaxFrequency(status.getLong("maxFreq", 0));
			List<ModulationConfig> configs = new ArrayList<>();
			configs.add(loraConfig);
			LoraStatus result = new LoraStatus();
			result.setStatus("IDLE");
			result.setDeviceStatus(DeviceConnectionStatus.CONNECTED);
			result.setConfigs(configs);
			return result;
		} catch (IOException e) {
			Util.logIOException(LOG, "unable to get status from: " + hostname, e);
			return new LoraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	@Override
	public LoraResponse startObservation(LoraObservationRequest req) {
		HttpRequest request = createRequest("/api/v2/lora/rx/start").header("Content-Type", "application/json").POST(BodyPublishers.ofString(toJson(req))).build();
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

	@Override
	public LoraResponse stopObservation() {
		HttpRequest request = createRequest("/api/v2/rx/stop").POST(BodyPublishers.noBody()).build();
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

	@Override
	public boolean isSupported() {
		return true;
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
		result.setRssi((short) obj.getFloat("rssi", 0));
		result.setSnr(obj.getFloat("snr", 0));
		result.setTimestamp(obj.getLong("timestamp", 0));
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
		json.add("ldo", req.getLdro());
		json.add("useCrc", req.isUseCrc() ? 1 : 0);
		json.add("useExplicitHeader", req.isUseExplicitHeader() ? 1 : 0);
		json.add("length", req.getBeaconSizeBytes());
		json.add("gain", req.getGain());
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
