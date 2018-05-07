package ru.r2cloud.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class R2CloudClient {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudClient.class);

	private final Configuration config;

	public R2CloudClient(Configuration config) {
		this.config = config;
	}

	public Long saveMeta(ObservationResult observation) {
		HttpURLConnection con = null;
		try {
			URL obj = new URL(config.getProperty("r2cloud.hostname") + "/api/v1/observation");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", config.getProperty("r2cloud.apiKey"));

			JsonObject json = convert(observation);
			Writer w = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
			json.writeTo(w);
			w.close();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to save meta. response code: " + responseCode + ". See logs for details");
				Util.toLog(LOG, con.getInputStream());
				return null;
			}
			return readObservationId(con);
		} catch (Exception e) {
			LOG.error("unable to save meta", e);
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	public void saveChannel(Long id, File getaPath) {
		upload("/api/v1/observation/" + id + "/data", getaPath);
	}

	public void saveSpectogram(Long id, File spectogramPath) {
		upload("/api/v1/observation/" + id + "/spectogram", spectogramPath);
	}

	private void upload(String url, File file) {
		HttpURLConnection con = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			URL obj = new URL(config.getProperty("r2cloud.hostname") + url);
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("PUT");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Authorization", config.getProperty("r2cloud.apiKey"));
			Util.copy(fis, con.getOutputStream());
			con.getOutputStream().flush();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to upload. response code: " + responseCode + ". See logs for details");
				Util.toLog(LOG, con.getInputStream());
			}
		} catch (Exception e) {
			LOG.error("unable to save meta", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					LOG.error("unable to close", e);
				}
			}
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private static Long readObservationId(HttpURLConnection con) throws IOException {
		JsonValue result = Json.parse(new InputStreamReader(con.getInputStream()));
		if (!result.isObject()) {
			LOG.info("malformed json");
			return null;
		}
		JsonObject resultObj = result.asObject();
		String status = resultObj.getString("status", null);
		if (status == null || !status.equalsIgnoreCase("SUCCESS")) {
			LOG.info("response error: " + resultObj);
			return null;
		}
		String idStr = resultObj.getString("id", null);
		if (idStr == null) {
			LOG.info("id not found: " + resultObj);
			return null;
		}
		return Long.valueOf(idStr);
	}

	private static JsonObject convert(ObservationResult observation) {
		JsonObject json = new JsonObject();
		json.add("satellite", observation.getSatelliteId());
		json.add("start", observation.getStart().getTime());
		json.add("end", observation.getEnd().getTime());
		json.add("gain", observation.getGain());
		json.add("channelA", observation.getChannelA());
		json.add("channelB", observation.getChannelB());
		if (observation.getNumberOfDecodedPackets() != null) {
			json.add("numberOfDecodedPackets", observation.getNumberOfDecodedPackets());
		}
		return json;
	}
}
