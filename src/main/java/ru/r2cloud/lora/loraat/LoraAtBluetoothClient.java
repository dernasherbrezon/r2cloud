package ru.r2cloud.lora.loraat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
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

public class LoraAtBluetoothClient implements LoraAtClient {

	private static final String MALFORMED_JSON = "MALFORMED_JSON";

	private static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";

	private static final Logger LOG = LoggerFactory.getLogger(LoraAtBluetoothClient.class);
	private final HttpClient httpclient;
	private final String urlPrefix;
	private final String blueetoothAddress;

	public LoraAtBluetoothClient(String hostport, String blueetoothAddress, int timeout) {
		this.urlPrefix = "http://" + hostport;
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(timeout)).build();
		this.blueetoothAddress = blueetoothAddress;
	}

	@Override
	public LoraStatus getStatus() {
		HttpRequest request = createRequest("/api/v1/status").GET().build();
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
			Util.logIOException(LOG, "unable to get r2lora status from: " + urlPrefix, e);
			return new LoraStatus(DeviceConnectionStatus.FAILED, CONNECTION_FAILURE);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	@Override
	public LoraResponse startObservation(LoraObservationRequest loraRequest) {
		// must be started by device already
		LoraResponse result = new LoraResponse();
		result.setStatus(ResponseStatus.SUCCESS);
		return result;
	}

	@Override
	public LoraResponse stopObservation() {
		HttpRequest request;
		request = createRequest("/api/v1/data?client=" + blueetoothAddress).GET().build();
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
		if (!json.isArray()) {
			LOG.info(MALFORMED_JSON);
			return new LoraResponse(MALFORMED_JSON);
		}
		JsonArray framesArray = json.asArray();
		LoraResponse result = new LoraResponse();
		result.setStatus(ResponseStatus.SUCCESS);
		List<LoraFrame> loraFrames = new ArrayList<>(framesArray.size());
		for (int i = 0; i < framesArray.size(); i++) {
			LoraFrame cur = readFrame(framesArray.get(i));
			if (cur == null) {
				continue;
			}
			loraFrames.add(cur);
		}
		result.setFrames(loraFrames);
		return result;
	}
	
	private static LoraFrame readFrame(JsonValue val) {
		if (!val.isObject()) {
			return null;
		}
		JsonObject obj = val.asObject();
		LoraFrame result = new LoraFrame();
		result.setData(Util.hexStringToByteArray(obj.getString("data", null)));
		result.setFrequencyError(obj.getFloat("frequencyError", 0));
		result.setRssi(obj.getFloat("rssi", 0));
		result.setSnr(obj.getFloat("snr", 0));
		result.setTimestamp(obj.getLong("timestamp", 0));
		return result;
	}

	private LoraStatus readStatus(String con) {
		JsonValue json;
		try {
			json = Json.parse(con);
		} catch (ParseException e) {
			LOG.info(MALFORMED_JSON);
			return new LoraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		if (!json.isArray()) {
			LOG.info(MALFORMED_JSON);
			return new LoraStatus(DeviceConnectionStatus.FAILED, MALFORMED_JSON);
		}
		JsonArray obj = json.asArray();
		for (int i = 0; i < obj.size(); i++) {
			JsonValue cur = obj.get(i);
			if (!cur.isObject()) {
				continue;
			}
			JsonObject curobj = cur.asObject();
			if (!curobj.getString("btaddress", "").equalsIgnoreCase(blueetoothAddress)) {
				continue;
			}
			LoraStatus result = new LoraStatus();
			result.setStatus("IDLE");
			result.setDeviceStatus(DeviceConnectionStatus.CONNECTED);

			ModulationConfig loraConfig = new ModulationConfig();
			loraConfig.setName("lora");
			loraConfig.setMinFrequency(curobj.getFloat("minFrequency", 0));
			loraConfig.setMaxFrequency(curobj.getFloat("maxFrequency", 0));

			List<ModulationConfig> configs = new ArrayList<>();
			configs.add(loraConfig);
			result.setConfigs(configs);
			return result;
		}
		return new LoraStatus(DeviceConnectionStatus.FAILED, "Not configured");
	}

	private HttpRequest.Builder createRequest(String path) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(urlPrefix + path));
		result.timeout(Duration.ofMinutes(1L));
		result.header("User-Agent", R2Cloud.getVersion() + " leosatdata.com");
		return result;
	}
}
